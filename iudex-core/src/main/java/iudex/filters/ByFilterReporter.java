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

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import iudex.core.Filter;
import iudex.core.FilterException;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Closeable;

/**
 * Maintains by filter counts of reject and failure rates.
 */
public final class ByFilterReporter implements FilterListener, Closeable
{
    /**
     * Keeps per filter rejected and failed counts.
     */
    public static final class Counter
    {
        public long rejected()
        {
            return _rejected.get();
        }

        public long failed()
        {
            return _failed.get();
        }

        public void countRejected()
        {
            _rejected.incrementAndGet();
        }

        public void countFailed()
        {
            _failed.incrementAndGet();
        }

        private final AtomicLong _rejected = new AtomicLong(0);
        private final AtomicLong _failed   = new AtomicLong(0);
    }

    /**
     * Receive report data periodically and at close. Calls to report are
     * internally synchronized.
     */
    public interface ReportWriter
    {
        public void report( long total, long delta, long duration,
                            Map<Filter,Counter> counters );
    }

    /**
     * Construct given index of filters, report writer, and report
     * period in seconds.
     */
    public ByFilterReporter( FilterIndex index,
                             ReportWriter writer,
                             double period_s )
    {
        Collection<Filter> filters = index.filters();
        _counters = new IdentityHashMap<Filter,Counter>( filters.size() );

        for( Filter f : filters ) {
            _counters.put( f, new Counter() );
        }

        _writer = writer;

        _notifier = new Notifier( period_s );
    }

    @Override
    public void accepted( UniMap result )
    {
        _notifier.tick();
    }

    @Override
    public void rejected( Filter filter, UniMap reject )
    {
        _counters.get( filter ).countRejected();
        _notifier.tick();
    }

    @Override
    public void failed( Filter filter, UniMap reject, FilterException x )
    {
        _counters.get( filter ).countFailed();
        _notifier.tick();
    }

    public long totalCount()
    {
        return _notifier.count();
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
        protected void notify( long total, long delta, long duration )
        {
            _writer.report( total, delta, duration, _counters );
        }
    }

    private final Notifier _notifier; //and accepted count
    private final IdentityHashMap< Filter, Counter > _counters;
    private final ReportWriter _writer;
}
