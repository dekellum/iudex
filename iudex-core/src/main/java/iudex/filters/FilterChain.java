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

import iudex.core.Described;
import iudex.core.Filter;
import iudex.core.FilterContainer;
import iudex.core.FilterException;

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
        _description = description;
        _filters = new ArrayList<Filter>( filters );
    }

    public void setListener( FilterListener listener )
    {
        _listener = listener;
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

            if( passed ) _listener.accepted( in );
            else         _listener.rejected( filter, in );
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
    public List<Object> describe()
    {
        return Arrays.asList( (Object) _description );
    }

    private final String _description;
    private final ArrayList<Filter> _filters;
    private FilterListener _listener = new NoOpListener();
}
