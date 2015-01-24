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

package iudex.filter.core;

import java.util.Arrays;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.filter.FilterException;

/**
 * Filter for simple copy from one key's value to another.
 *
 * @param <T> key type for copy compatibility.
 */
public final class Copier<T> implements Filter, Described
{
    public Copier( Key<? extends T> from, Key<T> to )
    {
        this( from, to, true );
    }

    public Copier( Key<? extends T> from, Key<T> to, boolean ifNull )
    {
        _from = from;
        _to = to;
        _ifNull = ifNull;
    }

    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        T value = content.get( _from );
        if ( ( value != null ) || _ifNull ) {
           content.set( _to, value );
        }
        return true;
    }

    @Override
    public List<?> describe()
    {
        return Arrays.asList( _from, _to, _ifNull );
    }

    private final Key<? extends T> _from;
    private final Key<T> _to;
    private final boolean _ifNull;
}
