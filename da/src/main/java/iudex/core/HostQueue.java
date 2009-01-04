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
            return prev.peek().compareTo( next.peek() );
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

    
    public void add( VisitOrder order )
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

    public VisitOrder peek()
    {
        return _work.peek();
    }
    
    public VisitOrder remove()
    {
        return _work.remove();
    }
    
    private final String _host;
    private long _nextVisit = System.currentTimeMillis();
    
    // FIXME: Logically a priority queue but may be more optimal 
    // as a simple linked list FIFO, as we already get work from database in
    // sorted priority order.
    private PriorityQueue<VisitOrder> _work = new PriorityQueue<VisitOrder>();
}
