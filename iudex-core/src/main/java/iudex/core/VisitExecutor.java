/*
 * Copyright (c) 2008-2010 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package iudex.core;

import iudex.filter.FilterContainer;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Closeable;

public class VisitExecutor implements Closeable, Runnable
{
    public VisitExecutor( FilterContainer chain, WorkPollStrategy poller )
    {
        _chain = chain;
        _poller = poller;
    }

    public synchronized void run()
    {
        try {
            while( true ) {
                long maxWait = checkWorkPoll();
                spawnThreads();
                wait( maxWait );
            }
        }
        catch( InterruptedException e ) {
            _log.warn( "Main run loop:", e );
        }
    }

    /**
     * Check if work needs to be polled either because this it the first time
     * or if maxWorkPollInterval has expired, by delegating to the
     * WorkPollStrategy. If strategy returns a new VisitQueue, then
     * shutdown existing worker threads.
     *
     * @return maximum time to wait in milliseconds before re-checking.
     */
    protected synchronized long checkWorkPoll()
    {
        long delta = _maxWorkPollInterval;
        long now = System.currentTimeMillis();

        if( ( _visitQ == null ) || ( now >= _nextPollTime ) ) {

            if( ( _visitQ == null ) || _poller.shouldReplaceQueue( _visitQ ) ) {
                shutdownWorkers();
                // FIXME: Blocks. Might be better, if safe?, to support overlap
                // of old and new generation visitors

                ++_generation;
                _threadCounter = 0;

                _visitQ = _poller.pollWork( null );
            }
            else {
                _poller.pollWork( _visitQ );
            }
            _nextPollTime = now + _maxWorkPollInterval;
        }
        else {
            delta = _nextPollTime - now;
        }
        return delta;
    }

    private synchronized void spawnThreads()
    {
        // Grow threads to "max"
        while( _threads.size() < _maxThreads ) {
            Thread t = new Visitor( _visitQ, _generation, ++_threadCounter );
            t.start();
            _threads.add( t );
        }
    }

    @Override
    public synchronized void close()
    {
        shutdownWorkers();
        _chain.close();
    }

    /**
     * Interrupt all worker threads and wait for them to gracefully exit.
     */
    private synchronized void shutdownWorkers()
    {
        for( Thread thread : _threads ) {
            thread.interrupt();
        }

        // Wait for all to exit
        try {
            long ellapsed = 0;
            long now = System.currentTimeMillis();
            while( ( _threads.size() > 0 ) &&
                   ( ellapsed < _maxShutdownWait ) ) {
                wait( _maxShutdownWait );
                long nextNow = System.currentTimeMillis();
                ellapsed += nextNow - now;
                now = nextNow;
            }
        }
        catch( InterruptedException x ) {
            _log.warn( "Shutdown interrupted: " + x );
        }

        if( _threads.size() > 0 ) {
            _log.warn( "({}) vistor threads remain after maxShutdownWait",
                       _threads.size() );
        }
    }

    private synchronized void threadExiting( Thread thread )
    {
        _log.debug( "Exit: {}", thread.getName() );
        _threads.remove( thread );
        notifyAll();
    }

    private class Visitor extends Thread
    {
        public Visitor( VisitQueue queue, int generation, int instance )
        {
            super( String.format( "visitor-%d-%d", generation, instance ) );
            _visitQ = queue;
        }

        @Override
        public void run()
        {
            try {
                while( true ) {

                    final HostQueue hq = _visitQ.take();
                    final long now = hq.lastTake();
                    try {
                        UniMap order = hq.remove();
                        order.set( ContentKeys.VISIT_START, new Date( now ) );
                        _chain.filter( order );
                    }
                    finally {
                        hq.setNextVisit( now + _minHostDelay );
                        _visitQ.untake( hq );
                    }
                }
            }
            catch( RuntimeException x ) {
                _log.error( "While processing: ", x );
            }
            catch( InterruptedException x ) {
                _log.debug( "Interrupted: ", x );
            }
            finally {
                threadExiting( Thread.currentThread() );
            }
        }

        private final VisitQueue _visitQ;
    }

    private int  _maxThreads          = 10;
    private long _minHostDelay        = 2000; //2s
    private long _maxWorkPollInterval = 10 * 60 * 1000; //10min
    private long _maxShutdownWait     = 20 * 1000; //20s

    private long _nextPollTime        = 0;

    private int _generation = 0;
    private int _threadCounter = 0;

    private final FilterContainer _chain;
    private final WorkPollStrategy _poller;

    private VisitQueue _visitQ = null;
    private Collection<Thread> _threads = new HashSet<Thread>();
    private Logger _log = LoggerFactory.getLogger( getClass() );
}
