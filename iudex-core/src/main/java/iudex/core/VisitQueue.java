/*
 * Copyright (c) 2008-2011 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package iudex.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.UniMap;

/**
 * A prioritized queue of ready and sleeping HostQueues. The ready
 * hosts queue is prioritized by the top priority order in each ready
 * host. The sleeping hosts queue is prioritized by least next visit
 * time.
 */
public class VisitQueue
{
    public int defaultMinHostDelay()
    {
        return _defaultMinHostDelay;
    }

    public void setDefaultMinHostDelay( int defaultMinHostDelay )
    {
        _defaultMinHostDelay = defaultMinHostDelay;
    }

    public int defaultMaxAccessPerHost()
    {
        return _defaultMaxAccessPerHost;
    }

    public void setDefaultMaxAccessPerHost( int defaultMaxAccessPerHost )
    {
        _defaultMaxAccessPerHost = defaultMaxAccessPerHost;
    }

    public synchronized void configureHost( String host,
                                            int minHostDelay,
                                            int maxAccessCount )
    {
        String key = hostKey( host.trim() );
        HostQueue hq = _hosts.get( key );
        if( hq != null ) {
            throw new IllegalStateException(
                "configureHost on non empty VisitQueue or " +
                hq.host() +
                " already configured." );
        }

        _hosts.put( key,
                    new HostQueue( key, minHostDelay, maxAccessCount ) );
    }

    public synchronized void addAll( List<UniMap> orders )
    {
        for( UniMap order : orders ) {
            privAdd( order );
        }
        notifyAll();
    }

    public synchronized void add( UniMap order )
    {
        privAdd( order );
        notifyAll();
    }

    /**
     * Return the total number of visit orders across all hosts.
     */
    public synchronized int orderCount()
    {
        return _orderCount;
    }

    /**
     * Return the total number of unique hosts for which there is at
     * least one visit order.
     */
    public synchronized int hostCount()
    {
        return _hosts.size();
    }

    /**
     * Take the next ready/highest priority host queue. May block
     * indefinitely for the next ready queue. Once a HostQueue is
     * returned, the calling thread owns it exclusively and must
     * guarantee to return it via untake() after removing the highest
     * priority visit order.
     * @deprecated Use acquire/release instead.
     */
    public HostQueue take() throws InterruptedException
    {
        return take( Long.MAX_VALUE );
    }

    /**
     * Take the next ready/highest priority host queue. May block up
     * to maxWait for the next ready queue. If a HostQueue is
     * returned, the calling thread owns it exclusively and must
     * guarantee to return it via untake() after removing the highest
     * priority visit order.
     * @param maxWait maximum wait in milliseconds
     * @return HostQueue or null if maxWait exceeded
     * @deprecated Use acquire/release instead.
     */
    public synchronized HostQueue take( long maxWait )
        throws InterruptedException
    {
        long now = System.currentTimeMillis();
        HostQueue ready = null;
        while( ( ( ready = _readyHosts.poll() ) == null ) &&
               ( maxWait > 0 ) ) {
            HostQueue next = null;
            while( ( next = _sleepHosts.peek() ) != null ) {
                if( ( now - next.nextVisit() ) >= 0 ) {
                    addReady( _sleepHosts.remove() );
                }
                else break;
            }
            if( _readyHosts.isEmpty() ) {

                long delay = maxWait;
                if( next != null ) {
                    delay = Math.min( next.nextVisit() - now + 1, maxWait );
                }
                wait( delay );
                long nextNow = System.currentTimeMillis();
                maxWait -= nextNow - now;
                now = nextNow;
            }
        }
        if( ready != null ) ready.setLastTake( now );
        return ready;
    }

    /**
     * Returns the highest priority of the available visit orders. May block
     * up to maxWait milliseconds. Caller must call the
     * {@link #release(UniMap, UniMap)}  when done processing this order.
     * @return UniMap visit order or null if maxWait was  exceeded
     */
    public synchronized UniMap acquire( long maxWait )
        throws InterruptedException
    {
        UniMap job = null;
        HostQueue hq = take( maxWait );
        if( hq != null ) {
            _log.debug( "Take: {}", hq.host() );

            job = hq.remove();
            untakeImpl( hq );
        }
        return job;
    }

    /**
     * Release reference on host/domain previously obtained by
     * {@link #acquire(long)}  and possibly add a new order.
     * @param acquired order with unchanged ContentKeys.URL from acquire.
     * @param newOrder optional, possibly new order to add with this release.
     */
    public synchronized void release( UniMap acquired, UniMap newOrder )
    {
        if( newOrder != null ) privAdd( newOrder );
        --_orderCount;

        HostQueue queue = _hosts.get( orderKey( acquired ) );
        _log.debug( "Release: {} {}", queue.host(), queue.size() );

        if( queue.release() ) {
            if( queue.size() > 0 ) {
                addSleep( queue );
            }
            else checkRemove( queue );
        }
    }

    /**
     * Return the previously taken HostQueue, after removing a single
     * visit order and adjusting the next visit time accordingly.
     * @deprecated Use acquire/release instead.
     */
    public synchronized void untake( HostQueue queue )
    {
        --_orderCount;
        queue.release();
        untakeImpl( queue );
        checkRemove( queue );
    }

    protected String orderKey( UniMap order )
    {
        return order.get( ContentKeys.URL ).domain();
    }

    protected String hostKey( String host )
    {
        host = Domains.normalize( host );
        String domain = Domains.registrationLevelDomain( host );
        return ( domain != null ) ? domain : host;
    }

    private void checkRemove( HostQueue queue )
    {
        if( ( queue.accessCount() == 0 ) &&
            ( queue.size() == 0 ) &&
            ( queue.minHostDelay() == _defaultMinHostDelay ) &&
            ( queue.maxAccessCount() == _defaultMaxAccessPerHost ) ) {

            _hosts.remove( queue.host() );
        }
    }

    private void untakeImpl( HostQueue queue )
    {
        if( queue.isAvailable() && ( queue.size() > 0 ) ) {
            addSleep( queue );
        }
    }

    private void privAdd( UniMap order )
    {
        String host = orderKey( order );

        HostQueue queue = _hosts.get( host );
        final boolean isNew = ( queue == null );
        if( isNew ) {
              queue = new HostQueue( host,
                                     _defaultMinHostDelay,
                                     _defaultMaxAccessPerHost );
              _hosts.put( host, queue );
        }

        queue.add( order );

        if( queue.size() == 1 ) addReady( queue );

        ++_orderCount;
    }

    private void addReady( HostQueue queue )
    {
        if( _log.isDebugEnabled() ) {
            _log.debug( "addReady: {} {}", queue.host(), queue.size() );
            checkAdd( queue );
        }
        _readyHosts.add( queue );
    }

    private void addSleep( HostQueue queue )
    {
        if( _log.isDebugEnabled() ) {
            _log.debug( "addSleep: {} {}", queue.host(), queue.size() );
            checkAdd( queue );
        }
        _sleepHosts.add( queue );
        notifyAll();
    }

    private void checkAdd( HostQueue queue )
        throws IllegalStateException
    {
        if( _readyHosts.contains( queue ) ) {
            throw new IllegalStateException( "Already ready!" );
        }
        if( _sleepHosts.contains( queue ) ) {
            throw new IllegalStateException( "Already sleeping!" );
        }
        if( queue.size() == 0 ) {
            throw new IllegalStateException( "Adding empty queue!" );
        }
    }

    private int _defaultMinHostDelay     = 500; //ms
    private int _defaultMaxAccessPerHost =   1;

    private int _orderCount = 0;
    private final Map<String, HostQueue> _hosts      =
        new HashMap<String, HostQueue>();

    private PriorityQueue<HostQueue>     _readyHosts =
        new PriorityQueue<HostQueue>( 1024, new HostQueue.TopOrderComparator());

    private PriorityQueue<HostQueue>     _sleepHosts =
        new PriorityQueue<HostQueue>( 128, new HostQueue.NextVisitComparator());

    private Logger _log = LoggerFactory.getLogger( getClass() );
}
