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

import java.util.IdentityHashMap;
import java.util.Map;

import iudex.core.Described;
import iudex.core.Filter;

/**
 * Maintains an index of Filter instances (by identity) to descriptive unique
 * names for use in logging or reporting. If a filter implements
 * {@link iudex.core.Described}, these tokens will be used to augment the filter
 * class name. Calls to {@link #name()} are unsynchronized. All calls to
 * {@link #register()} should be completed before the first call to
 * {@link #name()}.
 */
public class FilterIndex
{
    /**
     * Register a new unique name for the specified filter if not already
     * registered.
     * @return the registered name of filter
     */
    public synchronized String register( Filter filter )
    {
        String name = _filters.get( filter );
        if( name == null ) {
            //..generate name..
            String base = baseName( filter );
            name = base;
            int count = 1;
            while( _filters.values().contains( name ) ) {
                name = base + '-' + count++;
            }
            _filters.put( filter, name );
        }
        return name;
    }

    public String name( Filter filter )
    {
        String name = _filters.get( filter );
        if( name == null ) {
            name = unregisteredName( filter );
        }
        return name;
    }

    private String unregisteredName( Filter filter )
    {
        return filter.getClass().getName() + '@' +
               Integer.toHexString( System.identityHashCode( filter ) );
    }

    private String baseName( Filter filter )
    {
        StringBuilder name = new StringBuilder( 128 );

        name.append( compactClassName( filter ) );

        if( filter instanceof Described ) {
            for( Object value : ( (Described) filter ).describe() ) {
                name.append( '-' );
                name.append( value );
            }
        }

        return name.toString();
    }

    private String compactClassName( Object o )
    {
        return o.getClass().getName().replaceAll( "(\\w)\\w+\\.", "$1." );
    }

    private final Map<Filter,String> _filters =
        new IdentityHashMap<Filter,String>( 71 );
}
