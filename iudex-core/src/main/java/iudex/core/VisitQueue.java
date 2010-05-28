/*
 * Copyright (c) 2008-2010 David Kellum
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

import com.gravitext.htmap.UniMap;

/**
 * A prioritized queue of ready and sleeping HostQueues. The ready
 * hosts queue is prioritized by the top priority order in each ready
 * host. The sleeping hosts queue is prioritized by least next visit
 * time.
 */
public class VisitQueue
{
    public synchronized void addAll( List<UniMap> orders )
    {
        for( UniMap order : orders ) {
            privAdd( order );
        }
        _highHostCount = hostCount();
        _highOrderCount = orderCount();

        notifyAll();
    }

    public synchronized void add( UniMap order )
    {
        privAdd( order );
        _highHostCount = hostCount();
        _highOrderCount = orderCount();
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
     * Return ratio of total visit orders remaining since last add or addAll().
     */
    public synchronized float orderRemainingRatio()
    {
        return ( ( (float) orderCount() ) / ( (float) _highOrderCount ) );
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
     * Return ratio of unique hosts remaining since last add or addAll().
     */
    public synchronized float hostRemainingRatio()
    {
        return ( ( (float) hostCount() ) / ( (float) _highHostCount ) );
    }

    /**
     * Take the next ready/highest priority host queue. May block
     * indefinitely for the next ready queue. Once a HostQueue is
     * returned, the calling threads own its exclusively and must
     * guarantee to return it via untake() after removing the highest
     * priority visit order.
     */
    public HostQueue take() throws InterruptedException
    {
        return take( System.currentTimeMillis() );
    }

    /**
     * Take the next ready/highest priority host queue. May block
     * indefinitely for the next ready queue. Once a HostQueue is
     * returned, the calling threads own its exclusively and must
     * guarantee to return it via untake() after removing the highest
     * priority visit order.
     */
    public synchronized HostQueue take( long now ) throws InterruptedException
    {
        HostQueue ready = null;
        while( ( ready = _readyHosts.poll() ) == null ) {
            HostQueue next = null;
            while( ( next = _sleepHosts.peek() ) != null ) {
                if( ( now - next.nextVisit() ) >= 0 ) {
                    _readyHosts.add( _sleepHosts.remove() );
                }
                else
                    break;
            }
            if( _readyHosts.isEmpty() ) {
                long delay =
                    ( next == null ) ? 0 : ( next.nextVisit() - now + 1 );
                wait( delay );
                now = System.currentTimeMillis();
            }
        }
        ready.setLastTake( now );
        return ready;
    }

    /**
     * Return the previously taken HostQueue, after removing a single
     * visit order and adjusting the next visit time accordingly.
     */
    public synchronized void untake( HostQueue queue )
    {
        --_orderCount;
        if( queue.size() == 0 ) {
            _hosts.remove( queue.host() );
        }
        else {
            _sleepHosts.add( queue );
            notifyAll();
        }
    }

    private void privAdd( UniMap order )
    {
        String host = order.get( ContentKeys.URL ).host();
        HostQueue queue = _hosts.get( host );
        boolean isNew = false;

        if( queue == null ) {
            queue = new HostQueue( host );
            isNew = true;
        }

        queue.add( order );

        if( isNew ) {
            _hosts.put( host, queue );
            _readyHosts.add( queue );
        }

        ++_orderCount;
    }

    private int _orderCount = 0;
    private int _highHostCount;
    private int _highOrderCount;

    private final Map<String, HostQueue> _hosts      =
        new HashMap<String, HostQueue>();

    private PriorityQueue<HostQueue>     _readyHosts =
        new PriorityQueue<HostQueue>( 1024, new HostQueue.TopOrderComparator());

    private PriorityQueue<HostQueue>     _sleepHosts =
        new PriorityQueue<HostQueue>( 128, new HostQueue.NextVisitComparator());
}
