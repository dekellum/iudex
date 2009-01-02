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
    public void test() throws VisitURL.SyntaxException, InterruptedException
    {
        add_order( "http://h1/b", 0.1 );
        add_order( "http://h1/a", 0.2 );
        assertEquals( 2, _visitQ.size() );
        
        HostQueue hq = _visitQ.take();
        assertEquals( "http://h1/a", hq.remove().url().toString() );
        _visitQ.untake( hq );
        hq = _visitQ.take();
        assertEquals( "http://h1/b", hq.remove().url().toString() );
        _visitQ.untake( hq );
        
        assertEquals( 0, _visitQ.size() );
    }

    private void add_order( String url, double priority ) 
        throws VisitURL.SyntaxException
    {
        _visitQ.add( new VisitOrder( VisitURL.normalize( url ),
                                     VisitOrder.Type.PAGE,
                                     priority ) );
    }
}
