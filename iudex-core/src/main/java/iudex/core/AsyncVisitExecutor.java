/*
 * Copyright (c) 2011 David Kellum
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
import iudex.http.HostAccessListener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Closeable;

/**
 * A Visit Executor for use with an asynchronous HTTP client.
 */
public class AsyncVisitExecutor
    implements Closeable, Runnable, HostAccessListener
{
    public AsyncVisitExecutor( FilterContainer chain, WorkPollStrategy poller )
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

    public synchronized ThreadPoolExecutor startExecutor()
    {
        if( _executor == null ) {
            LinkedBlockingQueue<Runnable> queue =
                new LinkedBlockingQueue<Runnable>( _maxExecQueueCapacity );

            _executor = new ThreadPoolExecutor( _maxThreads, _maxThreads,
                                                30, TimeUnit.SECONDS,
                                                queue );
        }
        return _executor;
    }

    public synchronized void start()
    {
        if( _manager != null ) {
            throw new IllegalStateException( "Manager already started." );
        }
        _manager = new Thread( this, "manager" );

        startExecutor();

        if( _doShutdownHook ) {
            _shutdownHook = new ShutdownHook();
            Runtime.getRuntime().addShutdownHook( _shutdownHook );
        }

        _shutdown = false;

        _manager.start();
    }

    /**
     * Returns executor, only available after start() has returned.
     */
    public ThreadPoolExecutor executor()
    {
        return _executor;
    }

    public void join() throws InterruptedException
    {
        Thread executor = null;
        synchronized( this ) {
            executor = _manager;
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
    public void run()
    {
        //FIXME: Replace with Runnable for privacy?

        _running = true;

        try {
            final long maxTakeWait = maxTakeWait();
            long now = System.currentTimeMillis();
            while( _running ) {

                checkWorkPoll( now );

                final HostQueue hq = _visitQ.take( maxTakeWait );
                if( _running && ( hq != null ) ) {
                    now = hq.lastTake();

                    int count = acquireHost( hq.host(), hq );
                    if( count <= _maxAccessCount ) {
                        UniMap order = hq.remove();
                        order.set( ContentKeys.VISIT_START, new Date( now ) );

                        _executor.execute( new VisitTask( order ) );
                    }
                    else {
                        _log.warn( "Skipping host {} with access count {}",
                                   hq.host(), count );
                        // Access listener should still untake( queue ) on
                        // the last releaseHost()
                    }
                }
                else {
                    now = System.currentTimeMillis();
                }
            }
        }
        catch( InterruptedException x ) {
            _log.warn( "Executor run loop: " + x );
        }
        finally {
            _log.info( "Manager thread exit" );
            _manager = null;
            _running = false;
        }
    }

    @Override
    public void hostChange( String acquiredHost, String releasedHost )
    {
        synchronized( _hostCounts ) {
            if( acquiredHost != null ) {
                acquireHost( acquiredHost, null );
            }
            if( releasedHost != null ) {
                releaseHost( releasedHost );
            }
        }
    }

    /**
     * Check if work needs to be polled by delegating to the
     * WorkPollStrategy. If strategy returns a new VisitQueue, then await
     * for existing VisitTasks to complete.
     */
    private synchronized void checkWorkPoll( long now )
        throws InterruptedException
    {
        long delta = 0;

        if( _shutdown || now < _nextCheckWorkPoll ) {
            return;
        }

        if( _visitQ != null ) {
            delta = _poller.nextPollWork( _visitQ, now - _lastPollTime );
        }

        if( ( _visitQ == null ) || ( delta <= 0 ) ) {

            if( ( _visitQ == null ) || _poller.shouldReplaceQueue( _visitQ ) ) {

                if( ( _visitQ != null ) && _doWaitOnGeneration ) {
                    awaitExecutorEmpty();
                }

                ++_generation;

                _visitQ = _poller.pollWork( null );
            }
            else {
                _poller.pollWork( _visitQ );
            }
            now = System.currentTimeMillis();

            _lastPollTime = now;

            delta = _poller.nextPollWork( _visitQ, 0 );
        }

        _nextCheckWorkPoll = now + delta;
    }

    private class VisitTask implements Runnable
    {
        public VisitTask( UniMap order )
        {
            _order = order;
        }

        @Override
        public void run()
        {
            try {
                _chain.filter( _order );
            }
            catch( RuntimeException x ) {
                _log.error( "While processing: ", x );
            }
        }

        private final UniMap _order;
    }

    private long maxTakeWait()
    {
        long mwait = _minHostDelay + 10;
        mwait = Math.min( mwait, _maxShutdownWait - 10 );

        return mwait;
    }

    private int acquireHost( String host, HostQueue queue )
    {
       synchronized( _hostCounts ) {
           AccessCount count = _hostCounts.get( host );
           if( count == null ) {
               count = new AccessCount( 1 );
               _hostCounts.put( host, count );
           }
           else {
               count.adjust( 1 );
           }
           if( queue != null ) count.push( queue );
           return count.count();
       }
    }

    private void releaseHost( String host )
    {
       synchronized( _hostCounts ) {
           AccessCount count = _hostCounts.get( host );
           if( count.adjust( -1 ) == 0 ) {
               HostQueue queue = count.pop();
               if( queue != null ) {
                   queue.setNextVisit( queue.lastTake() + _minHostDelay );
                   _visitQ.untake( queue );
               }
           }
       }
    }

    private static final class AccessCount
    {
        AccessCount( int count )
        {
            _count = count;
        }

        public int adjust( int adjustment )
        {
            _count += adjustment;
            return _count;
        }

        public void push( HostQueue queue )
        {
            _queue = queue;
        }

        public HostQueue pop()
        {
            HostQueue q = _queue;
            _queue = null;
            return q;
        }

        public int count()
        {
            return _count;
        }

        private int _count;
        private HostQueue _queue = null;
    }

    private synchronized void awaitExecutorEmpty() throws InterruptedException
    {
        //FIXME: Or use custom task that notify's?
        // * Or really shutdown, as empty queue does not guarantee completion?
        // * Shutdown only guaranteed if HTTPClient uses same pool?
        // * Executor can't be restarted after, so implies new HTTPClient on
        //   each generation?!?
        //FIXME: Or support true asynchronous work polling by pre-filtering
        // work by pending URL.

        long now = System.currentTimeMillis();
        long end = now + _maxShutdownWait;
        while( now < end ) {
            if( _executor.getQueue().isEmpty() ) break;
            wait( Math.min( 50, end - now ) );
            now = System.currentTimeMillis();
        }
        if( now >= end ) {
            _log.warn( "Excutor not empty after {}ms", _maxShutdownWait );
        }
        else {
            // FIXME: Lame additional padding, hoping for any remaining
            // HTTP + chain jobs to timeout
            wait( end - now  );
        }
    }

    private void shutdown( boolean fromVM )
        throws InterruptedException
    {
        Thread manager = null;

        synchronized( this ) {
            _shutdown = true;         //Avoid more visitors

            //Shutdown executor
            _running = false;
            notifyAll();
            manager = _manager;
        }

        if( manager != null ) {
            manager.join( _maxShutdownWait );
        }

        synchronized( this ) {
            if( _manager != null ) {
                _log.warn(  "Manager not exiting: interrupt" );
                _manager.interrupt();
            }
        }

        //FIXME: Is this really how we want to do this?
        _executor.shutdown();
        _executor.awaitTermination( _maxShutdownWait,
                                    TimeUnit.MILLISECONDS );

        if( !fromVM ) {
            synchronized( this ) {
                if( _shutdownHook != null ) {
                    Runtime.getRuntime().removeShutdownHook( _shutdownHook );
                    _shutdownHook = null;
                }
            }
        }
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
    private ThreadPoolExecutor _executor = null;

    private Map<String,AccessCount> _hostCounts =
        new HashMap<String,AccessCount>( 8 * 1024 );

    private long _nextCheckWorkPoll = 0;

    private int  _maxThreads              = 10;
    private long _minHostDelay            =  2 * 1000; //2s
    private long _maxShutdownWait         = 19 * 1000; //19s
    private boolean _doWaitOnGeneration   = false;
    private boolean _doShutdownHook       = true;
    private int   _maxExecQueueCapacity   = 1000;
    private int   _maxAccessCount         = 2;

    private Thread _manager               = null;
    private ShutdownHook _shutdownHook    = null;
    private volatile boolean _running     = false;
    private volatile boolean _shutdown    = true;

    private long _lastPollTime            = 0;
    private int _generation               = 0;

    private VisitQueue _visitQ            = null;

    private Logger _log = LoggerFactory.getLogger( getClass() );
}
