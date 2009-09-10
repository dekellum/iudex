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

import iudex.filter.Filter;
import iudex.filter.FilterException;
import iudex.filter.FilterListener;

import com.gravitext.htmap.UniMap;

import org.slf4j.MDC;

/**
 * Remove specified key from SLF4J Mapped Diagnostic Context as it exits a
 * filter chain.
 */
public final class MDCUnsetter implements FilterListener
{
    /**
     * Construct given key.toString() to remove from MDC.
     */
    public MDCUnsetter( Object key )
    {
        _key = key.toString();
    }

    @Override
    public void accepted( UniMap result )
    {
        unset();
    }

    @Override
    public void failed( Filter filter, UniMap reject, FilterException x )
    {
        unset();
    }

    @Override
    public void rejected( Filter filter, UniMap reject )
    {
        unset();
    }

    public void unset()
    {
        MDC.remove( _key );
    }

    private final String _key;
}
