/*
 * Copyright (c) 2008-2011 David Kellum
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
 * Filter for simply setting a static value.
 *
 * @param <T> value type to be set
 */
public final class Setter<T> implements Filter, Described
{
    public Setter( Key<? super T> to, T value )
    {
        _to = to;
        _value = value;
    }

    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        content.set( _to, _value );
        return true;
    }

    @Override
    public List<?> describe()
    {
        return Arrays.asList( _to, _value );
    }

    private final Key<? super T> _to;
    private final T _value;
}
