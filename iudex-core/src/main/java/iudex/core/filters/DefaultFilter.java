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

import java.util.Date;
import java.util.List;

import com.gravitext.htmap.UniMap;

import iudex.filter.Filter;
import static iudex.core.ContentKeys.*;

/**
 * Sets default values: VISIT_START (to now, if not already set) and the REFERER
 * and VISIT_START of any REFERENCES.
 */
public class DefaultFilter implements Filter
{
    @Override
    public boolean filter( UniMap content )
    {
        Date start = content.get( VISIT_START );
        if( start == null ) {
            start = new Date();
            content.set( VISIT_START, start );
        }

        final List<UniMap> refs = content.get( REFERENCES );
        if( refs != null ) {
            for( UniMap ref : refs ) {
                ref.set( REFERER, content );
                ref.set( VISIT_START, start );
            }
        }
        return true;
    }

}
