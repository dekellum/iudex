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

import iudex.core.HostQueue;
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
            Content order = hq.remove();
            assertEquals( url, order.get( ContentKeys.URL ).toString() );
        }
        finally {
            hq.setNextVisit( System.currentTimeMillis() + 50 );
            _visitQ.untake( hq );
        }
    }

    private void addOrder( String url, double priority ) 
        throws VisitURL.SyntaxException
    {
        Content content = new Content();
        content.set( ContentKeys.URL, VisitURL.normalize( url ) );
        content.set( ContentKeys.TYPE, ContentKeys.Type.PAGE );
        content.set( ContentKeys.PRIORITY, priority );
        
        _visitQ.add( content );
    }
}
