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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 * @author David Kellum
 */
public class PeriodicNotifier
{
    /**
     * Construct given target notification period in seconds. Actual 
     * notification time may vary from [⅔∙period_s, ∞] depending on input
     * tick rate. In general high tick()/period ratio yields more stable
     * notifications.
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
        final State state = _state.get();

        if( ( ( tick - state.prior ) % state.next ) == 0 ) {
            if( _checkLock.tryLock() ) {
                try {
                    check( tick, state );
                }
                finally {
                    _checkLock.unlock();
                }
            }
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
    private final void check( final long tick, final State state )
    {
        final long ticks = tick - state.prior;
        final long now = System.nanoTime();
        final long duration = now - _beginning;

        if( duration >= _minPeriod ) {
            long next = ticks * _period / duration;
            _state.set( new State( tick, next ) );
            _beginning = now;
            notify( tick, ticks, duration );
        }
        else {
            long next = ticks * ( _period - duration ) / duration;
            _state.set( new State( state.prior, next ) );        
        }
    }

    private final static class State
    {
        State()
        {
            this( 0L, 1L );
        }
        
        State( long prior, long next )
        {
            this.prior = prior;
            this.next = next;
        }

        final long prior;
        final long next;
    }
    
    private final long _period;    //ns
    private final long _minPeriod;

    private final ReentrantLock _checkLock = new ReentrantLock( false );
    private long _beginning = System.nanoTime();
    
    private final AtomicLong _count = new AtomicLong(0);
    private final AtomicReference<State> _state =
        new AtomicReference<State>( new State() );
}
