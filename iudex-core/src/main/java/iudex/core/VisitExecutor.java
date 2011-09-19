/*
 * Copyright (c) 2008-2011 David Kellum
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
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.util.Closeable;

/**
 * Custom threaded executor for processing VisitQueues.
 */
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

    public void setMaxShutdownWait( long maxShutdownWait )
    {
        _maxShutdownWait = maxShutdownWait;
    }

    public void setDoWaitOnGeneration( boolean doWaitOnGeneration )
    {
        _doWaitOnGeneration = doWaitOnGeneration;
    }

    public synchronized void start()
    {
        if( _executor != null ) {
            throw new IllegalStateException( "Executor already started." );
        }

        _executor = new Thread( this, "executor" );

        if( _doShutdownHook ) {
            _shutdownHook = new ShutdownHook();
            Runtime.getRuntime().addShutdownHook( _shutdownHook );
        }

        _shutdown = false;

        _executor.start();
    }

    public void join() throws InterruptedException
    {
        Thread executor = null;
        synchronized( this ) {
            executor = _executor;
        }
        if( executor != null ) executor.join();
    }

    public void shutdown() throws InterruptedException
    {
        shutdown( false );
    }

    /**
     * Shutdown executor and visitor threads. Note the provided filter chain
     * should be closed externally.
     */
    @Override
    public synchronized void close()
    {
        try {
            shutdown( false );
        }
        catch( InterruptedException x ) {
            _log.warn( "On executor shutdown: " + x );
        }
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
     * Check if work needs to be polled by delegating
     * to the WorkPollStrategy. If strategy returns a new VisitQueue,
     * then shutdown existing worker threads.
     *
     * @return maximum time to wait in milliseconds before re-checking.
     */
    protected synchronized long checkWorkPoll()
    {
        long now = System.currentTimeMillis();
        long delta = 0;

        if( _shutdown ) {
            return 100;
        }

        if( _visitQ != null ) {
            delta = _poller.nextPollWork( _visitQ, now - _lastPollTime );
        }

        if( ( _visitQ == null ) || ( delta <= 0 ) ) {

            if( ( _visitQ == null ) || _poller.shouldReplaceQueue( _visitQ ) ) {

                shutdownVisitors( _doWaitOnGeneration );

                ++_generation;
                _threadCounter = 0;

                _visitQ = _poller.pollWork( null );
            }
            else {
                _poller.pollWork( _visitQ );
            }

            _lastPollTime = now;
            delta = _poller.nextPollWork( _visitQ, 0 );
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

    private void shutdown( boolean fromVM )
        throws InterruptedException
    {
        Thread executor = null;

        synchronized( this ) {
            _shutdown = true;         //Avoid more visitors
            shutdownVisitors( true ); //wait

            //Shutdown executor
            _running = false;
            notifyAll();
            executor = _executor;
        }

        if( executor != null ) {
            executor.join( _maxShutdownWait );
        }

        synchronized( this ) {
            if( _executor != null ) {
                _log.warn(  "Executor not exiting: interrupt" );
                _executor.interrupt();
            }
        }

        if( !fromVM ) {
            synchronized( this ) {
                if( _shutdownHook != null ) {
                    Runtime.getRuntime().removeShutdownHook( _shutdownHook );
                    _shutdownHook = null;
                }
            }
        }
    }

    /**
     * Interrupt all visitor threads and wait for them to gracefully
     * exit.
     */
    private synchronized void shutdownVisitors( boolean doWait )
    {
        if( _visitors.size() > 0 ) {
            _log.debug( "Shutdown of current generation visitors." );
        }

        for( Visitor visitor : _visitors ) {
            visitor.shutdown();
        }

        if( doWait ) {
            if( _visitors.size() > 0 ) {
                _log.debug( "Waiting for visitors to exit." );
            }

            try {
                waitForVisitorExit( _maxShutdownWait );

                if( _visitors.size() > 0 ) {
                    _log.warn( "Interrupting remaining visitors." );
                    for( Visitor visitor : _visitors ) {
                        visitor.interrupt();
                    }
                    waitForVisitorExit( _maxPostInterruptWait );
                }
            }
            catch( InterruptedException x ) {
                _log.warn( "Shutdown interrupted: " + x );
            }
        }
    }

    private synchronized void waitForVisitorExit( long maxWait )
        throws InterruptedException
    {
        long ellapsed = 0;
        long now = System.currentTimeMillis();
        while( ( _visitors.size() > 0 ) && ( ellapsed < maxWait ) ) {
            wait( maxWait );
            long nextNow = System.currentTimeMillis();
            ellapsed += nextNow - now;
            now = nextNow;
        }
        if( _visitors.size() > 0 ) {
            _log.warn( "{} visitor threads remain after waiting {}ms",
                       _visitors.size(), maxWait );
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
                        try {
                            _chain.filter( hq.remove() );
                        }
                        finally {
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
            mwait = Math.min( mwait, _maxShutdownWait - 10 );

            return mwait;
        }

        private final VisitQueue _visitQ;
        private volatile boolean _running = true;
    }

    private class ShutdownHook extends Thread
    {
        public ShutdownHook()
        {
            super( "visitor-shutdown" );
        }

        @Override
        public void run()
        {
            _log.info( "Visitor shutdown hook closing" );
            try {
                shutdown( true );
                //FIXME: Need a better solution for allow main to exit.
                Thread.sleep( 1000 );
            }
            catch( InterruptedException x ) {
                _log.warn( "On shutdown: " + x );
            }
        }
    }

    private final FilterContainer _chain;
    private final WorkPollStrategy _poller;

    private int  _maxThreads              = 10;
    private long _minHostDelay            =  2 * 1000; //2s
    private long _maxShutdownWait         = 19 * 1000; //19s
    private long _maxPostInterruptWait    =  1 * 1000;
    private boolean _doWaitOnGeneration   = false;
    private boolean _doShutdownHook       = true;

    private Thread _executor              = null;
    private ShutdownHook _shutdownHook    = null;
    private volatile boolean _running     = false;
    private volatile boolean _shutdown    = true;

    private long _lastPollTime            = 0;
    private int _generation               = 0;
    private int _threadCounter            = 0;

    private VisitQueue _visitQ            = null;
    private Collection<Visitor> _visitors = new HashSet<Visitor>();

    private Logger _log = LoggerFactory.getLogger( getClass() );
}
