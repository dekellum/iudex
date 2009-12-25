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

package iudex.da.feed;

import iudex.filter.Described;
import iudex.filter.Filter;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

/**
 * Filter that removes a date value if it is beyond a specified grace
 * period in the future.
 */
public final class FutureDateFilter implements Filter, Described
{
    /**
     * Construct to filter the specified field.
     */
    public FutureDateFilter( Key<Date> field )
    {
        _field = field;
        setGracePeriod( 10, TimeUnit.MINUTES );
    }

    /**
     * Set grace period in future beyond which the value will be removed
     * (Default: 10 minutes).
     */
    public void setGracePeriod( long period, TimeUnit unit )
    {
        _grace = TimeUnit.MILLISECONDS.convert( period, unit );
    }

    @Override
    public List<?> describe()
    {
        return Arrays.asList( _field.toString() );
    }

    @Override
    public boolean filter( UniMap content )
    {
        final Date date = content.get( _field );
        if( date != null ) {
            final long time = date.getTime();

            // If this date is before some previously obtained "now" +
            // grace, then its known good, otherwise recheck with an
            // updated "now."
            if( time > ( _lazyNow.get() + _grace ) ) {
                final long now = System.currentTimeMillis();
                _lazyNow.set( now );
                if( time > ( now + _grace ) ) {
                    content.remove( _field );
                }
            }
        }
        return true;
    }

    private final Key<Date> _field;
    private final AtomicLong _lazyNow =
        new AtomicLong( System.currentTimeMillis() );
    private long _grace; // milliseconds

}
