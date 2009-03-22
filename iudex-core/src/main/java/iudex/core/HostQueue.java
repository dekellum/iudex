/*
 * Copyright (C) 2008-2009 David Kellum
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
import java.util.PriorityQueue;

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
    
    public HostQueue( String host )
    {
        _host = host;
    }

    public long nextVisit()
    {
        return _nextVisit;
    }

    public void setNextVisit( long nextVisitMillis )
    {
        _nextVisit = nextVisitMillis;
    }

    
    public void add( Content order )
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

    public Content peek()
    {
        return _work.peek();
    }
    
    public Content remove()
    {
        return _work.remove();
    }

    /**
     * Order by descending priority. 
     */
    private static final class PriorityComparator 
        implements Comparator<Content>
    {
        public int compare( Content prev, Content next )
        {
            return Float.compare( next.get( ContentKeys.PRIORITY ),
                                  prev.get( ContentKeys.PRIORITY ) );
        }
        
    }
    private static final PriorityComparator PRIORITY_COMPARATOR =
        new PriorityComparator();
    private final String _host;
    private long _nextVisit = 0; 
    
    // FIXME: Logically a priority queue but may be more optimal 
    // as a simple linked list FIFO, as we already get work from database in
    // sorted priority order.
    private PriorityQueue<Content> _work = 
        new PriorityQueue<Content>( 256, PRIORITY_COMPARATOR );
}
