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

    public void setMaxThreads( int maxThreads )
    {
        _maxThreads = maxThreads;
    }

    public void setMinHostDelay( long minHostDelay )
    {
        _minHostDelay = minHostDelay;
    }

    public void setMaxWorkPollInterval( long maxWorkPollInterval )
    {
        _maxWorkPollInterval = maxWorkPollInterval;
    }

    public void setMaxShutdownWait( long maxShutdownWait )
    {
        _maxShutdownWait = maxShutdownWait;
    }

    public void setDoWaitOnGeneration( boolean doWaitOnGeneration )
    {
        _doWaitOnGeneration = doWaitOnGeneration;
    }

    public void start()
    {
        if( _executor != null ) {
            throw new IllegalStateException( "Executor already started." );
        }
        _executor = new Thread( this, "executor" );

        _executor.start();
    }

    public void join() throws InterruptedException
    {
        _executor.join();
    }

    public synchronized void shutdown() throws InterruptedException
    {
        _running = false;
        shutdownVisitors( true );
        if( _executor != null ) _executor.join();
    }

    /**
     * Shutdown executor and visitor threads, then close filter chain.
     */
    @Override
    public synchronized void close()
    {
        try {
            shutdown();
        }
        catch( InterruptedException x ) {
            _log.warn( "On executor shutdown: " + x );
        }

        _chain.close();
    }

    @Override
    public synchronized void run()
    {
        //FIXME: Really want to expose this as public? Replace with Runnable?

        _running = true;

        try {
            while( _running ) {
                long maxWait = checkWorkPoll();
                spawnThreads();
                wait( maxWait );
            }
        }
        catch( InterruptedException x ) {
            _log.warn( "Executor run loop: " + x );
        }
        finally {
            _executor = null;
            _running = false;
        }
    }

    /**
     * Check if work needs to be polled either because this it the
     * first time or if maxWorkPollInterval has expired, by delegating
     * to the WorkPollStrategy. If strategy returns a new VisitQueue,
     * then shutdown existing worker threads.
     *
     * @return maximum time to wait in milliseconds before re-checking.
     */
    protected synchronized long checkWorkPoll()
    {
        long delta = _maxWorkPollInterval;
        long now = System.currentTimeMillis();

        if( ( _visitQ == null ) || ( now >= _nextPollTime ) ) {

            if( ( _visitQ == null ) || _poller.shouldReplaceQueue( _visitQ ) ) {

                shutdownVisitors( _doWaitOnGeneration );

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
        while( !_shutdown && ( _visitors.size() < _maxThreads ) ) {
            Visitor v = new Visitor( _visitQ, _generation, ++_threadCounter );
            _visitors.add( v );
            v.start();
        }
    }

    /**
     * Interrupt all visitor threads and wait for them to gracefully
     * exit.
     */
    private synchronized void shutdownVisitors( boolean doWait )
    {
        for( Visitor thread : _visitors ) {
            thread.shutdown();
            //FIXME: Interrupt if stopRunning doesn't work?
            //if( doWait ) thread.interrupt();
        }

        // Wait for all to exit
        try {
            if( doWait ) {
                _shutdown = true;
                long ellapsed = 0;
                long now = System.currentTimeMillis();
                while( ( _visitors.size() > 0 ) &&
                       ( ellapsed < _maxShutdownWait ) ) {
                    wait( _maxShutdownWait );
                    long nextNow = System.currentTimeMillis();
                    ellapsed += nextNow - now;
                    now = nextNow;
                }

                if( _visitors.size() > 0 ) {
                    _log.warn( "({}) visitor threads after maxShutdownWait",
                               _visitors.size() );
                }
            }
        }
        catch( InterruptedException x ) {
            _log.warn( "Shutdown interrupted: " + x );
        }
        finally {
            _shutdown = false;
        }
    }

    private synchronized void threadExiting( Visitor visitor )
    {
        _log.debug( "Exit: {}", visitor.getName() );
        _visitors.remove( visitor );
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
                _log.debug( "Running" );
                final long maxTakeWait = maxTakeWait();
                while( true ) {
                    final HostQueue hq = _visitQ.take( maxTakeWait );
                    if( ! _running ) break;
                    if( hq != null ) {
                        final long now = hq.lastTake();
                        try {
                            UniMap order = hq.remove();
                            order.set( ContentKeys.VISIT_START,
                                       new Date( now ) );
                            _chain.filter( order );
                        }
                        finally {
                            hq.setNextVisit( now + _minHostDelay );
                            _visitQ.untake( hq );
                        }
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
                threadExiting( this );
            }
        }

        public void shutdown()
        {
            _running = false;
        }

        private long maxTakeWait()
        {
            long mwait = _minHostDelay + 10;
            mwait = Math.min( mwait, _maxWorkPollInterval - 10 );
            mwait = Math.min( mwait, _maxShutdownWait - 10 );

            return mwait;
        }

        private final VisitQueue _visitQ;
        private volatile boolean _running = true;
    }

    private final FilterContainer _chain;
    private final WorkPollStrategy _poller;
    private Thread _executor          = null;
    private volatile boolean _running = false;

    private int  _maxThreads          = 10;
    private long _minHostDelay        = 2000; //2s
    private long _maxWorkPollInterval = 10 * 60 * 1000; //10min
    private long _maxShutdownWait     = 20 * 1000; //20s
    private boolean _doWaitOnGeneration = false;
    private volatile boolean _shutdown = false;

    private long _nextPollTime        = 0;
    private int _generation           = 0;
    private int _threadCounter        = 0;

    private VisitQueue _visitQ            = null;
    private Collection<Visitor> _visitors = new HashSet<Visitor>();

    private Logger _log = LoggerFactory.getLogger( getClass() );
}
