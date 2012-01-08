/*
 * Copyright (c) 2008-2012 David Kellum
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

package iudex.filter.core;

import static org.junit.Assert.*;
import iudex.filter.core.PeriodicNotifier;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.concurrent.TestExecutor;
import com.gravitext.concurrent.TestFactoryBase;
import com.gravitext.concurrent.TestRunnable;
import com.gravitext.util.FastRandom;
import com.gravitext.util.Metric;

public class PeriodicNotifierTest
{
    @Before
    public void setup()
    {
        _notifier = new Notifier();
    }

    @Test
    public void testConcurrentFrequency()
    {
        int ticks = 3000;
        long count = TestExecutor.run( new TickGenerator(), ticks, 3 );

        _log.debug( "{} ticks, total wait: {}s, notified: {}, dev: {}s",
                    new Object[] {
                        ticks,
                        Metric.format( count / 1e6d ),
                        _notifier.count(),
                        Metric.format( _notifier.dev() ) } );

        assertTrue( _notifier.count() > 0 );
    }

    private class TickGenerator extends TestFactoryBase
    {
        @Override
        public TestRunnable createTestRunnable( final int seed )
        {
            return new TestRunnable() {
                final FastRandom _rand = new FastRandom( seed );

                @Override
                public int runIteration( int run ) throws Exception
                {
                    int wait = _rand.nextInt( TICK_MAX );
                    Thread.sleep( wait / ( 1000 * 1000 ),
                                  wait % ( 1000 * 1000 ) );
                    _notifier.tick();
                    return wait / 1000;
                }

            };
        }
    }

    private class Notifier extends PeriodicNotifier
    {
        Notifier()
        {
            super( TARGET_PERIOD / 1e9d );
        }

        @Override
        protected void notify( long tick, long ticks, long duration )
        {
            assertTrue( tick > 0 );
            assertTrue( ticks > 0 );
            assertTrue( ticks <= tick );

            assertTrue( tick > _lastTick );
            _lastTick = tick;

            assertTrue( duration >= TARGET_PERIOD * 2 / 3 );

            long d = ( TARGET_PERIOD - duration );
            _sumSq += d * d;
            ++_count;
        }

        public double dev()
        {
            return Math.sqrt( ( (double) _sumSq ) / _count ) / 1e9d;
        }

        public long count()
        {
            return _count;
        }

        private long _sumSq = 0;
        private long _count = 0;
        private long _lastTick = 0;
    }
    private static long TARGET_PERIOD = 20 * 1000 * 1000; // 20ms
    private static  int TICK_MAX      =       500 * 1000; //   500µs

    private Notifier _notifier;
    private Logger _log = LoggerFactory.getLogger( getClass() );
}
