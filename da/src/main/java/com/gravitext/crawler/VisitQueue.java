package com.gravitext.crawler;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;


/**
 * 
 * The readyHosts queue is prioritized by the top FetchOrder in each ready host.
 * The sleeping host queue is prioritize by least next visit. 
 */
public class VisitQueue
{
    //FIXME: List<FetchOrder> orders instead.
    public void add( VisitOrder order )
    {
        String host = order.url().host();
        HostQueue queue = _hosts.get( host );
        
        if( queue == null ) {
            queue = new HostQueue( host );
            _hosts.put( host, queue );
            _readyHosts.add( queue );  // A new host is always ready
        }
        queue.add( order ); // FIXME: Before _readyHosts add?
        ++_size;
    }

    public int size()
    {
        return _size;
    }
    
    public HostQueue take() throws InterruptedException
    {
        if( _readyHosts.isEmpty() ) makeReady();
        HostQueue ready = _readyHosts.take();
        return ready;
    }
    
    public void untake( HostQueue queue )
    {
        if( queue.size() == 0 ) {
            _hosts.remove( queue.host() );
        }
        else {
            _sleepHosts.add( queue );
        }
        --_size;
    }

    public void makeReady()
    {
        Date now = new Date();
        HostQueue next = null;
        while( ( next = _sleepHosts.poll() ) != null ) {
            if( now.compareTo( next.nextVisit() ) >= 0 ) {
                _readyHosts.add( next );
            }
            else {
                _sleepHosts.add( next );
                break;
            }
        }
    }
    
    private final Map<String,HostQueue> _hosts = 
        new HashMap<String,HostQueue>(); 
    //FIXME: ConcurrentHashMap?
    
    private int _size = 0;

    //FIXME: Using Blocking aspect?
    private PriorityBlockingQueue<HostQueue> _readyHosts = 
        new PriorityBlockingQueue<HostQueue>
            ( 1024, new HostQueue.TopOrderComparator() );

    private PriorityQueue<HostQueue> _sleepHosts = 
        new PriorityQueue<HostQueue>
            ( 128, new HostQueue.NextVisitComparator() );
}
