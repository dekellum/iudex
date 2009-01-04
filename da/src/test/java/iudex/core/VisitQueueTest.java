package iudex.core;

import iudex.core.HostQueue;
import iudex.core.VisitOrder;
import iudex.core.VisitQueue;
import iudex.core.VisitURL;

import org.junit.Test;
import static org.junit.Assert.*;

public class VisitQueueTest
{
    private VisitQueue _visitQ = new VisitQueue();

    @Test
    public void testPriority() 
        throws VisitURL.SyntaxException, InterruptedException
    {
        addOrder( "http://h1/3", 1.3 );
        addOrder( "http://h1/1", 1.1 );
        addOrder( "http://h1/2", 1.2 );
        
        assertEquals( 3, _visitQ.size() );
        
        assertTakeNext( "http://h1/3" );
        assertTakeNext( "http://h1/2" );
        assertTakeNext( "http://h1/1" );
        
        assertEquals( 0, _visitQ.size() );
    }

    @Test
    public void testHosts() 
        throws VisitURL.SyntaxException, InterruptedException
    {
        addOrder( "http://h2/2", 2.2 );
        addOrder( "http://h2/1", 2.1 );
        addOrder( "http://h3/2", 3.2 );
        addOrder( "http://h3/1", 3.1 );
        addOrder( "http://h1/2", 1.2 );
        addOrder( "http://h1/1", 1.1 );
        
        assertEquals( 6, _visitQ.size() );
        
        assertTakeNext( "http://h3/2" );
        assertTakeNext( "http://h2/2" );
        assertTakeNext( "http://h1/2" );
        assertTakeNext( "http://h3/1" );
        assertTakeNext( "http://h2/1" );
        assertTakeNext( "http://h1/1" );
        
        assertEquals( 0, _visitQ.size() );
    }

    
    
    private void assertTakeNext( String url ) throws InterruptedException
    {
        Thread.sleep( 30 );
        
        final HostQueue hq = _visitQ.take(); 
        try {
            VisitOrder order = hq.remove();
            
            assertEquals( url, order.url().toString() );
        }
        finally {
            hq.setNextVisit( System.currentTimeMillis() + 50 );
            _visitQ.untake( hq );
        }
    }

    private void addOrder( String url, double priority ) 
        throws VisitURL.SyntaxException
    {
        _visitQ.add( new VisitOrder( VisitURL.normalize( url ),
                                     VisitOrder.Type.PAGE,
                                     priority ) );
    }
}
