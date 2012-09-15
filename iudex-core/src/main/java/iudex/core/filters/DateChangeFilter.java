/*
 * Copyright (c) 2008-2012 David Kellum
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

package iudex.core.filters;

import static iudex.core.ContentKeys.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import iudex.filter.Described;
import iudex.filter.Filter;

import com.gravitext.htmap.UniMap;

/**
 * Filter to reject content that has a publication date less than
 * changeCutoff milliseconds after the existing publication date.
 *
 * As a side-effect the filter method sets the REF_PUB_DELTA key
 * to the delta.
 */
public class DateChangeFilter implements Filter, Described
{
    public DateChangeFilter( boolean doFilter )
    {
        _doFilter = doFilter;
    }

    public void setChangeCutoff( long changeCutoff ) throws IllegalArgumentException
    {
        if(changeCutoff <= 0L)
        {
            throw new IllegalArgumentException("Invalid changeCutoff value");
        }
        _changeCutoff = changeCutoff;
    }

    @Override
    public boolean filter( UniMap content )
    {
        UniMap current = content.get( CURRENT );
        if( current != null ) {

            Date prior  = current.get( REF_PUB_DATE );
            Date update = content.get( REF_PUB_DATE );

            if( ( prior != null ) && ( update != null ) ) {

                long diffMs = update.getTime() - prior.getTime();
                content.set( REF_PUB_DELTA, (float) diffMs );

                return ( !_doFilter || ( diffMs >= _changeCutoff ) );
            }
        }
        return true;
    }

    @Override
    public List<?> describe()
    {
        return Arrays.asList( _doFilter );
    }

    private final boolean _doFilter;
    private long _changeCutoff = 1000L; // milliseconds
}
