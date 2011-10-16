/*
 * Copyright (c) 2008-2011 David Kellum
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

import iudex.filter.AsyncFilterContainer;
import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.filter.FilterContainer;
import iudex.filter.FilterException;
import iudex.filter.FilterListener;
import iudex.filter.NoOpListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Closeables;

public class FilterChain
    implements FilterContainer, Described
{
    public FilterChain( String description,
                        List<Filter> filters )
    {
        _description = Arrays.asList( description );
        _filters = new ArrayList<Filter>( filters );

        final int end = _filters.size();
        Filter filter = null;
        int i = 0;
        boolean foundAsync = false;
        while( i < end ) {
            filter = _filters.get( i++ );
            if( filter instanceof AsyncFilterContainer ) {
                foundAsync = true;
                break;
            }
        }
        if( foundAsync && ( i != end ) ) {
            throw new IllegalArgumentException(
                "Attempt to create chain with async. filter [" +
                filter.toString() +
                "] not at end of chain.");
        }
        _notifyPassed = !foundAsync;
    }

    public void setListener( FilterListener listener )
    {
        _listener = listener;

        if( hasSameFilter( this ) ) _notifyPassed = false;
    }

    public FilterListener listener()
    {
        return _listener;
    }

    @Override
    public boolean filter( UniMap in )
    {
        boolean passed = true;
        final int end = _filters.size();
        Filter filter = null;
        try {
            for( int i = 0; passed && i < end; ++i ) {
                filter = _filters.get( i );
                passed = filter.filter( in );
            }

            if( _notifyPassed ) {
                if( passed ) _listener.accepted( in );
                else         _listener.rejected( filter, in );
            }
        }
        catch( FilterException x ) {
            _listener.failed( filter, in, x );
            passed = false;
        }
        return passed;
    }

    @Override
    public List<Filter> children()
    {
        return Collections.unmodifiableList( _filters );
    }

    @Override
    public void close()
    {
        for( Filter f : _filters ) {
            Closeables.closeIf( f );
        }
    }

    @Override
    public List<?> describe()
    {
        return _description;
    }

    private boolean hasSameFilter( FilterContainer fc )
    {
        for( Filter sf : fc.children() ) {
            if( ( sf instanceof FilterChain ) &&
                ( _listener == ( (FilterChain) sf ).listener() ) ) {
                return true;
            }
            if( ( sf instanceof FilterContainer ) &&
                hasSameFilter( (FilterContainer) sf ) ) {
                return true;
            }
        }
        return false;
    }

    private final List<String> _description;
    private final ArrayList<Filter> _filters;
    private boolean _notifyPassed;
    private FilterListener _listener = new NoOpListener();
}
