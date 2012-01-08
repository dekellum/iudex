/*
 * Copyright (c) 2008-2012 David Kellum
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

package iudex.filter.core;

import java.util.Arrays;
import java.util.List;

import org.slf4j.MDC;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import iudex.filter.Described;
import iudex.filter.Filter;

/**
 * Apply specified key as a SLF4J Mapped Diagnostic Context key/value.
 */
public class MDCSetter implements Filter, Described
{

    public MDCSetter( Key key )
    {
        _key = key;
    }

    @Override
    public List<?> describe()
    {
        return Arrays.asList( _key );
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean filter( UniMap content )
    {
        Object value = content.get( _key );
        if( value != null ) {
            MDC.put( _key.name(), value.toString() );
        } else {
            MDC.remove( _key.name() );
        }
        return true;
    }

    private Key _key;

}
