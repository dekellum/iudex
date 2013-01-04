/*
 * Copyright (c) 2008-2013 David Kellum
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
import java.util.Iterator;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.filter.FilterContainer;

public class SubListFilter implements FilterContainer, Described
{
    public SubListFilter( Key< List<UniMap> > subKey,
                          FilterChain subChain )
    {
        this( subKey, subChain, true );
    }

    public SubListFilter( Key< List<UniMap> > subKey,
                          FilterChain subChain,
                          boolean removeRejects )
    {
        _subKey = subKey;
        _subChain = subChain;
        _removeRejects = removeRejects;
    }

    @Override
    public List<?> describe()
    {
        return Arrays.asList( _subKey, _removeRejects );
    }

    @Override
    public List<? extends Filter> children()
    {
        return _subChain.children();
    }

    @Override
    public boolean filter( UniMap map )
    {
        List<UniMap> subList = map.get( _subKey );

        if( subList != null ) {
            Iterator<UniMap> iter = subList.iterator();
            while( iter.hasNext() ) {
                boolean rejected = ! _subChain.filter( iter.next() );
                if( _removeRejects && rejected ) iter.remove();
            }

            // Remove entire subList if empty
            if( subList.isEmpty() ) map.remove( _subKey );
        }

        return true;
    }

    @Override
    public void close()
    {
        _subChain.close();
    }

    private final Key< List<UniMap> > _subKey;
    private final FilterChain _subChain;
    private final boolean _removeRejects;
}
