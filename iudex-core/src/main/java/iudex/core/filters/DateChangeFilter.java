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

package iudex.core.filters;

import static iudex.core.ContentKeys.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import iudex.filter.Described;
import iudex.filter.Filter;

import com.gravitext.htmap.UniMap;

public class DateChangeFilter implements Filter, Described
{
    public DateChangeFilter( boolean doFilter )
    {
        _doFilter = doFilter;
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

                return ( !_doFilter || ( diffMs >= 1000L ) );
                //FIXME: Make difference cutoff (1000ms) a property.
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
}
