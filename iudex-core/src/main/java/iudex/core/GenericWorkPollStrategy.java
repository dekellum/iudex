/*
 * Copyright (c) 2008-2010 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package iudex.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.util.Metric;

/**
 * Generic abstract WorkPollStrategy implementation.
 */
public abstract class GenericWorkPollStrategy
    implements WorkPollStrategy
{
    /**
     * Set the minimum allowable milliseconds between successive work
     * polls.
     */
    public void setMinPollInterval( long minPollInterval )
    {
        _minPollInterval = minPollInterval;
    }

    public long getMinPollInterval()
    {
        return _minPollInterval;
    }

    /**
     * Set the maximum milliseconds between checking if a poll should
     * be made.
     */
    public void setMaxCheckInterval( long maxCheckInterval )
    {
        _maxCheckInterval = maxCheckInterval;
    }

    public long maxCheckInterval()
    {
        return _maxCheckInterval;
    }

    /**
     * Set the maximum milliseconds between successive work polls.
     */
    public void setMaxPollInterval( long maxPollInterval )
    {
        _maxPollInterval = maxPollInterval;
    }

    public long maxPollInterval()
    {
        return _maxPollInterval;
    }

    /**
     * The minimum ratio of hosts remaining since the last poll before
     * a new work poll will be made (subject to minPollInterval).
     */
    public void setMinHostRemainingRatio( float minHostRemainingRatio )
    {
        _minHostRemainingRatio = minHostRemainingRatio;
    }

    public float minHostRemainingRatio()
    {
        return _minHostRemainingRatio;
    }

    /**
     * The minimum ratio of orders (across all hosts) remaining since
     * the last poll before a new work poll will be made (subject to
     * minPollInterval). Note that zero total remaining orders always
     * triggers a new poll.
     */
    public void setMinOrderRemainingRatio( float minOrderRemainingRatio )
    {
        _minOrderRemainingRatio = minOrderRemainingRatio;
    }

    public float minOrderRemainingRatio()
    {
        return _minOrderRemainingRatio;
    }

    @Override
    public VisitQueue pollWork( VisitQueue current )
    {
        VisitQueue vq = new VisitQueue();

        pollWorkImpl( vq );

        _highOrderCount = vq.orderCount();
        _highHostCount = vq.hostCount();

        _log.info( "Polled {} orders in {} hosts",
                   _highOrderCount, _highHostCount );

        return vq;
    }

    /**
     * The actual pollWorkImpl which should be implemented to fill the
     * provided out queue.
     */
    public abstract void pollWorkImpl( VisitQueue out );

    /**
     * {@inheritDoc}
     * This implementation always returns true;
     */
    @Override
    public boolean shouldReplaceQueue( VisitQueue current )
    {
        return true;
    }

    @Override
    public long nextPollWork( VisitQueue current, long elapsed )
    {
        if( current == null ) return 0;

        final int oCount = current.orderCount();
        final int hCount = current.hostCount();
        final float oRatio = ratio( oCount, _highOrderCount );
        final float hRatio = ratio( hCount, _highHostCount );

        long wait = 0;

        if( ( oCount == 0 ) ||
            ( hRatio < _minHostRemainingRatio ) ||
            ( oRatio < _minOrderRemainingRatio ) ) {
            wait = Math.max( 0, _minPollInterval - elapsed );
        }
        else {
            wait = Math.min( _maxPollInterval - elapsed, _maxCheckInterval );
        }
        if( ( wait > 0 ) && _log.isDebugEnabled() ) {
            _log.debug( "orders {} ({}), hosts {} ({}); wait {}s",
                        new Object[] { Metric.format( oCount ),
                                       Metric.formatDifference( oRatio ),
                                       Metric.format( hCount ),
                                       Metric.formatDifference( hRatio ),
                                       Metric.format( (double) wait /
                                                      1000d ) } );
        }

        return wait;
    }

    protected float ratio( int count, int highMark )
    {
        return ( ( (float) count ) / ( (float) highMark ) );
    }

    private long _minPollInterval  =      15 * 1000; //15sec
    private long _maxCheckInterval =      60 * 1000; //1min;
    private long _maxPollInterval  = 10 * 60 * 1000; //10min

    private int _highHostCount = 0;
    private int _highOrderCount = 0;

    private float _minHostRemainingRatio  = 0.25f;
    private float _minOrderRemainingRatio = 0.10f;

    private Logger _log = LoggerFactory.getLogger( getClass() );
}
