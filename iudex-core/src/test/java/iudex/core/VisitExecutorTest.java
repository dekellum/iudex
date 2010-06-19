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

import iudex.core.VisitURL.SyntaxException;
import iudex.filter.NoOpFilter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.UniMap;

public class VisitExecutorTest
{
    class TestStrategy implements WorkPollStrategy
    {

        @Override
        public VisitQueue pollWork( VisitQueue current )
        {
            assertNull( current );

            VisitQueue q = new VisitQueue();

            ++_batch;

            _log.info( "Creating VisitQueue {}", _batch );

            q.add( order( 1, _batch, 1, 1.4f ) );
            q.add( order( 1, _batch, 2, 1.3f ) );
            q.add( order( 2, _batch, 1, 1.5f ) );
            q.add( order( 2, _batch, 2, 1.4f ) );
            q.add( order( 3, _batch, 1, 1.6f ) );
            q.add( order( 3, _batch, 2, 1.5f ) );
            q.add( order( 4, _batch, 1, 1.7f ) );
            q.add( order( 4, _batch, 2, 1.6f ) );

            return q;
        }

        @Override
        public boolean shouldReplaceQueue( VisitQueue visitQ )
        {
            return true;
        }

        public int _batch = 0;
    }

    class TestFilter extends NoOpFilter
    {
        @Override
        public boolean filter( UniMap content )
        {
            _log.info(  "Filtered {} {}",
                        content.get( ContentKeys.URL ), _latch.getCount() );
            try {
                Thread.sleep( 3 );
            }
            catch( InterruptedException e ) {
            }

            _latch.countDown();
            return true;
        }
    }

    @Before
    public void setup()
    {
        _latch = new CountDownLatch( 20 );

        _executor = new VisitExecutor( new TestFilter(), new TestStrategy() );
        _executor.setMaxThreads( 3 );
        _executor.setMinHostDelay( 100 );
        _executor.setMaxShutdownWait( 200 );
        _executor.setMaxWorkPollInterval( 130 );
        _executor.setDoWaitOnGeneration( false );
    }

    @After
    public void shutdown()
    {
        _executor.close();
    }

    @Test
    public void test() throws InterruptedException
    {
        _executor.start();
        assertTrue( _latch.await( 5, TimeUnit.SECONDS ) );
        _executor.shutdown();
    }

    private UniMap order( int host, int batch, int instance, float priority )
    {
        try {
            String url = String.format( "http://h%d.com/%d/%d",
                                        host, batch, instance );
            UniMap content = new UniMap();
            content.set( ContentKeys.URL, VisitURL.normalize( url ) );
            content.set( ContentKeys.TYPE, ContentKeys.TYPE_PAGE );
            content.set( ContentKeys.PRIORITY, priority );
            return content;
        }
        catch( SyntaxException x ) {
            throw new RuntimeException( x );
        }

    }

    private CountDownLatch _latch = null;
    private VisitExecutor _executor = null;
    private Logger _log = LoggerFactory.getLogger( getClass() );
}
