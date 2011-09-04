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

import java.util.Comparator;
import java.util.Date;
import java.util.PriorityQueue;

import com.gravitext.htmap.UniMap;

public class HostQueue
{
    public static class NextVisitComparator implements Comparator<HostQueue>
    {
        public int compare( HostQueue prev, HostQueue next )
        {
            return Long.signum( prev.nextVisit() - next.nextVisit() );
        }
    }

    public static class TopOrderComparator implements Comparator<HostQueue>
    {
        public int compare( HostQueue prev, HostQueue next )
        {
            return PRIORITY_COMPARATOR.compare( prev.peek(), next.peek() );
        }
    }

    public HostQueue( String host, long minHostDelay, int maxAccessCount )
    {
        _host = host;
        _minHostDelay = minHostDelay;
        _maxAccess = maxAccessCount;
    }

    /**
     * @deprecated
     */
    public long lastTake()
    {
        return _lastTake;
    }

    public void setLastTake( long now )
    {
        _lastTake = now;
        _nextVisit = _lastTake + _minHostDelay;
    }

    public long nextVisit()
    {
        return _nextVisit;
    }

    public void add( UniMap order )
    {
        _work.add( order );
    }

    public String host()
    {
        return _host;
    }

    public int size()
    {
        return _work.size();
    }

    public UniMap peek()
    {
        return _work.peek();
    }

    public UniMap remove()
    {
        ++_accessCount;
        UniMap order = _work.remove();
        order.set( ContentKeys.VISIT_START, new Date( _lastTake ) );
        return order;
    }

    public int accessCount()
    {
        return _accessCount;
    }

    public boolean isAvailable()
    {
        return ( _accessCount < _maxAccess );
    }

    public boolean release()
    {
        return ( --_accessCount < _maxAccess );
    }

    /**
     * Order by descending priority.
     */
    private static final class PriorityComparator
        implements Comparator<UniMap>
    {
        public int compare( UniMap prev, UniMap next )
        {
            return Float.compare( next.get( ContentKeys.PRIORITY ),
                                  prev.get( ContentKeys.PRIORITY ) );
        }

    }
    private static final PriorityComparator PRIORITY_COMPARATOR =
        new PriorityComparator();

    private final String _host;
    private final long _minHostDelay;
    private final int _maxAccess;

    private long _nextVisit = 0;

    private long _lastTake = 0;
    private int  _accessCount = 0;

    private PriorityQueue<UniMap> _work =
        new PriorityQueue<UniMap>( 256, PRIORITY_COMPARATOR );

}
