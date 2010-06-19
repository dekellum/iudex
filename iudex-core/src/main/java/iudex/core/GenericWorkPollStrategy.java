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

public abstract class GenericWorkPollStrategy
    implements WorkPollStrategy
{
    public void setMinPollInterval( long minPollInterval )
    {
        _minPollInterval = minPollInterval;
    }

    public void setMaxCheckInterval( long maxCheckInterval )
    {
        _maxCheckInterval = maxCheckInterval;
    }

    public void setMaxPollInterval( long maxPollInterval )
    {
        _maxPollInterval = maxPollInterval;
    }

    public void setMinHostRemainingRatio( float minHostRemainingRatio )
    {
        _minHostRemainingRatio = minHostRemainingRatio;
    }

    public void setMinOrderRemainingRatio( float minOrderRemainingRatio )
    {
        _minOrderRemainingRatio = minOrderRemainingRatio;
    }

    @Override
    public VisitQueue pollWork( VisitQueue current )
    {
        VisitQueue vq = new VisitQueue();

        pollWorkImpl( vq );

        return vq;
    }

    public abstract void pollWorkImpl( VisitQueue out );

    @Override
    public boolean shouldReplaceQueue( VisitQueue current )
    {
        return true;
    }

    public long nextPollWork( VisitQueue current, long ellapsed )
    {
        if( current == null ) return 0;

        if( ( current.orderCount() == 0 ) ||
            ( current.hostRemainingRatio() < _minHostRemainingRatio ) ||
            ( current.orderRemainingRatio() < _minOrderRemainingRatio ) ) {
            return Math.max( 0, _minPollInterval - ellapsed );
        }

        return Math.min( _maxPollInterval - ellapsed, _maxCheckInterval );
    }

    private long _minPollInterval  =      15 * 1000; //15sec
    private long _maxCheckInterval =      60 * 1000; //1min;
    private long _maxPollInterval  = 10 * 60 * 1000; //10min

    private float _minHostRemainingRatio  = 0.25f;
    private float _minOrderRemainingRatio = 0.10f;
}
