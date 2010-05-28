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
    public VisitExecutor( FilterContainer chain )
    {
        _chain = chain;
        _visitQ = new VisitQueue();
    }

    public synchronized void run()
    {
        try {
            while( true ) {
                spawnThreads();
                long maxWait = pollWork();
                wait( maxWait );
            }
        }
        catch( InterruptedException e ) {
            _log.warn( "Main run loop:", e );
        }
    }

    /**
     * Possibly poll work, and return maximum time to wait in milliseconds
     * before re-checking.
     */
    protected synchronized long pollWork()
    {
        long now = System.currentTimeMillis();
        long delta = _nextPollTime - now;
        if( delta <= 0 ) {
            //FIXME: Actually poll.
            _nextPollTime = now + _workPollInterval;
            return _workPollInterval;
        }
        return delta;
    }

    private synchronized void spawnThreads()
    {
        // Grow threads to "max"
        while( _threads.size() < _maxThreads ) {
            Thread t = new Thread( new Runner( _visitQ ),
                                   "visitor-" + ( _threads.size() + 1 ) );
            // base-1 thread#, reserve 0 for detached executor thread.
            t.start();
            _threads.add( t );
        }
    }

    @Override
    public void close()
    {
        //Add thread interupt/shutdown?

        _chain.close(); //FIXME: Really want this here?
    }

    private synchronized void threadExiting( Thread thread )
    {
        _log.debug( "Exit: {}", thread.getName() );
        _threads.remove( thread );
        notifyAll();
    }

    private class Runner implements Runnable
    {
        public Runner( VisitQueue queue )
        {
            _visitQ = queue;
        }

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

        private VisitQueue _visitQ; // FIXME: Here because it may change?
    }

    private int _minHostDelay = 2000;
    private int _workPollInterval = 10 * 60 * 1000; //10m in milliseconds
    private long _nextPollTime = 0;

    private int _maxThreads = 10;
    private final FilterContainer _chain;
    private VisitQueue _visitQ = null;
    private Collection<Thread> _threads = new HashSet<Thread>();
    private Logger _log = LoggerFactory.getLogger( getClass() );
}
