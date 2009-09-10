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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iudex.filter.Filter;
import iudex.filter.FilterException;
import iudex.filter.FilterListener;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Closeable;
import com.gravitext.util.Metric;

public final class SummaryReporter implements FilterListener, Closeable
{

    /**
     * Construct given descriptive name and report period in seconds.
     */
    public SummaryReporter( String name, double period_s )
    {
        _notifier = new Notifier( period_s );
        _log = LoggerFactory.getLogger( getClass().getName() + '.' + name );
    }

    @Override
    public void accepted( UniMap result )
    {
        _notifier.tick();
    }

    @Override
    public void rejected( Filter filter, UniMap reject )
    {
        _rejected.incrementAndGet();
        _notifier.tick();
    }

    @Override
    public void failed( Filter filter, UniMap reject, FilterException x )
    {
        _failed.incrementAndGet();
        _notifier.tick();
    }

    public long totalCount()
    {
        return _notifier.count();
    }

    public long acceptedCount()
    {
        return totalCount() - rejectedCount() - failedCount();
    }

    public long rejectedCount()
    {
        return _rejected.get();
    }

    public long failedCount()
    {
        return _failed.get();
    }

    @Override
    public void close()
    {
        _notifier.notifyNow();
    }

    private final class Notifier extends PeriodicNotifier
    {
        Notifier( double period_s )
        {
            super( period_s );
        }

        @Override
        protected void notify( long total, long tDelta, long duration )
        {
            long rejected = _rejected.get();
            long failed   = _failed.get();
            long accepted = total - rejected - failed;

            long aDelta = accepted - _lastAccepted;
            _lastAccepted = accepted;

            double tRate  = ( (double) tDelta   ) / duration * 1e9d;
            double aRate  = ( (double) aDelta   ) / duration * 1e9d;
            double aRatio = ( (double) accepted ) / total * 100.0d;

            _log.info( String.format(
                "T: %s %s/s A: %s %3.0f%% %s/s R: %s F: %s",
                Metric.format( total ),
                Metric.format( tRate ),
                Metric.format( accepted ),
                aRatio,
                Metric.format( aRate ),
                Metric.format( rejected ),
                Metric.format( failed ) ) );
        }

        private long _lastAccepted = 0;
    }

    private final AtomicLong _rejected = new AtomicLong(0);
    private final AtomicLong _failed   = new AtomicLong(0);

    private final Notifier _notifier; //and accepted count
    private Logger _log;
}
