/*
 * Copyright (c) 2008-2015 David Kellum
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
import com.gravitext.util.Stopwatch;

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

    public void setVisitQueueFactory( VisitQueueFactory factory )
    {
        _visitQueueFactory = factory;
    }

    @Override
    public VisitQueue pollWork( VisitQueue vq )
    {
        int oldOrders = 0;
        if( ( vq == null ) || shouldReplaceQueue( vq ) ) {
            vq = _visitQueueFactory.createVisitQueue();
        }
        else {
            oldOrders = vq.orderCount();
        }

        Stopwatch sw = new Stopwatch().start();
        pollWorkImpl( vq );
        sw.stop();

        _highOrderCount = vq.orderCount();
        _highHostCount = vq.hostCount();

        log().info( "Polled {} orders {} {} hosts; ({})",
                    _highOrderCount - oldOrders,
                    (oldOrders > 0) ? "in up to" : "in",
                    _highHostCount,
                    sw.duration() );
        return vq;
    }

    @Override
    public void discard( VisitQueue current )
    {
        log().info( "Discard of {} orders ignored", current.orderCount() );
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
        if( ( wait > 0 ) && log().isDebugEnabled() ) {
            log().debug( "orders {} ({}), hosts {} ({}), acq {}; wait {}s",
                         Metric.format( oCount ),
                         Metric.formatDifference( oRatio ),
                         Metric.format( hCount ),
                         Metric.formatDifference( hRatio ),
                         Metric.format( current.acquiredCount() ),
                         Metric.format( (double) wait / 1000d ) );
        }

        return wait;
    }

    protected float ratio( int count, int highMark )
    {
        return ( ( (float) count ) / ( (float) highMark ) );
    }

    protected Logger log()
    {
        return _log;
    }

    private VisitQueueFactory _visitQueueFactory = new VisitQueueFactory();

    private long _minPollInterval  =      15 * 1000; //15sec
    private long _maxCheckInterval =      30 * 1000; //30sec;
    private long _maxPollInterval  = 10 * 60 * 1000; //10min

    private int _highHostCount = 0;
    private int _highOrderCount = 0;

    private float _minHostRemainingRatio  = 0.25f;
    private float _minOrderRemainingRatio = 0.10f;

    private Logger _log = LoggerFactory.getLogger( getClass() );
}
