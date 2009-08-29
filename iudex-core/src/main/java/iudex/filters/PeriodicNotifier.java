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

package iudex.filters;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Notify on a target time period given variable input tick events. The
 * implementation is thread safe, high concurrency, low overhead.
 * @author David Kellum
 */
public class PeriodicNotifier
{
    /**
     * Construct given target notification period in seconds. Actual
     * notification interval may vary in the range [⅔∙period_s, ∞] depending on
     * input tick rate. In general high tick rate/period ratio yields more
     * accurate interval.
     */
    PeriodicNotifier( double period_s )
    {
        _period    = (long) ( period_s * 1e9d );                   //ns
        _minPeriod = (long) ( period_s * 1e9d * ( 2.0d / 3.0d ) ); //ns
    }

    /**
     * Increment count and possibly call notify.
     */
    public final void tick()
    {
        final long tick = _count.incrementAndGet();

        if( tick >= _next.get() ) {

            // If another thread has already picked this check up, then ignore.
            // Otherwise, this is the thread to check
            if( _checkLock.tryLock() ) {
                try {
                    check( _count.get(), false );
                }
                finally {
                    _checkLock.unlock();
                }
            }
        }
    }

    /**
     * Force immediate notify.
     */
    public final void notifyNow()
    {
        _checkLock.lock();
        try {
            check( _count.get(), true );
        }
        finally {
            _checkLock.unlock();
        }
    }

    /**
     * Perform some periodic action. This implementation does nothing. This
     * call is guaranteed synchronized by an internal lock.
     * @param tick The absolute count of calls to tick() on which this notify
     * was called.
     * @param ticks The number of ticks since the last call to notify.
     * @param duration The time in nanoseconds since the last call to notify.
     */
    protected void notify( long tick, long ticks, long duration )
    {
    }

    /**
     * Check if time to notify, and update tick state.
     * Guarded via _checkLock
     */
    private final void check( final long tick, final boolean force )
    {
        final long ticks = tick - _lastTick;
        final long now = System.nanoTime();
        final long duration = now - _start;

        if( force || ( duration >= _minPeriod ) ) {
            _next.set( tick + ( ticks * _period / duration ) );

            _start = now;
            _lastTick = tick;

            notify( tick, ticks, duration );
        }
        else {
            _next.set( tick + ( ticks * ( _period - duration ) / duration ) );
        }
    }

    private final long _period;    //ns
    private final long _minPeriod;

    private final AtomicLong _count = new AtomicLong(0);
    private final AtomicLong _next  = new AtomicLong(1);

    private final ReentrantLock _checkLock = new ReentrantLock( false );
    private long _start = System.nanoTime();
    private long _lastTick = 0;
}
