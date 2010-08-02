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

import iudex.core.HostQueue;
import iudex.core.VisitQueue;
import iudex.core.VisitURL;

import org.junit.Test;

import com.gravitext.htmap.UniMap;

import static org.junit.Assert.*;

public class VisitQueueTest
{
    private VisitQueue _visitQ = new VisitQueue();

    @Test
    public void testPriority()
        throws VisitURL.SyntaxException, InterruptedException
    {
        addOrder( "http://h1/3", 1.3F );
        addOrder( "http://h1/1", 1.1F );
        addOrder( "http://h1/2", 1.2F );

        assertEquals( 3, _visitQ.orderCount() );

        assertTakeNext( "http://h1/3" );
        assertTakeNext( "http://h1/2" );
        assertTakeNext( "http://h1/1" );

        assertEquals( 0, _visitQ.orderCount() );
    }

    @Test
    public void testHosts()
        throws VisitURL.SyntaxException, InterruptedException
    {
        addOrder( "http://h2/2", 2.2F );
        addOrder( "http://h2/1", 2.1F );
        addOrder( "http://h3/2", 3.2F );
        addOrder( "http://h3/1", 3.1F );
        addOrder( "http://h1/2", 1.2F );
        addOrder( "http://h1/1", 1.1F );

        assertEquals( 6, _visitQ.orderCount() );

        assertTakeNext( "http://h3/2" );
        assertTakeNext( "http://h2/2" );
        assertTakeNext( "http://h1/2" );
        assertTakeNext( "http://h3/1" );
        assertTakeNext( "http://h2/1" );
        assertTakeNext( "http://h1/1" );

        assertEquals( 0, _visitQ.orderCount() );
    }

    private void assertTakeNext( String url ) throws InterruptedException
    {
        Thread.sleep( 30 );

        final HostQueue hq = _visitQ.take( 200 );
        try {
            UniMap order = hq.remove();
            assertEquals( url, order.get( ContentKeys.URL ).toString() );
        }
        finally {
            hq.setNextVisit( System.currentTimeMillis() + 50 );
            _visitQ.untake( hq );
        }
    }

    private void addOrder( String url, float priority )
        throws VisitURL.SyntaxException
    {
        UniMap content = new UniMap();
        content.set( ContentKeys.URL, VisitURL.normalize( url ) );
        content.set( ContentKeys.TYPE, ContentKeys.TYPE_PAGE );
        content.set( ContentKeys.PRIORITY, priority );

        _visitQ.add( content );
    }
}
