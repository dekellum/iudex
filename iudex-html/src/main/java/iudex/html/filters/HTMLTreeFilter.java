/*
 * Copyright (c) 2010 David Kellum
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

package iudex.html.filters;

import java.util.Arrays;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;
import com.gravitext.xml.tree.Element;

import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.filter.FilterException;
import iudex.html.tree.TreeFilter;
import iudex.html.tree.TreeFilter.Action;
import iudex.html.tree.TreeWalker;

/**
 * Filter to apply a TreeFilter (or chain) to an HTML parse tree.
 */
public class HTMLTreeFilter implements Filter, Described
{
    public enum Order
    {
        BREADTH_FIRST,
        DEPTH_FIRST
    }

    public HTMLTreeFilter( Key<Element> tree,
                           TreeFilter filter )
    {
        this( tree, filter, Order.DEPTH_FIRST );
    }

    public HTMLTreeFilter( Key<Element> tree,
                           TreeFilter filter,
                           Order order )
    {
        _treeKey = tree;
        _filter = filter;
        _order = order;
    }

    @Override
    public List<?> describe()
    {
        return Arrays.asList( _treeKey, _order );

        /* FIXME: Any way to get subfilter reporting in here?
        List<? extends TreeFilter> children =
            Collections.singletonList( _filter );
        if( _filter instanceof TreeFilterChain ) {
            children = ((TreeFilterChain) _filter).children();
        }

        List<Object> tfilters = new ArrayList<Object>();
        tfilters.add( _treeKey );
        tfilters.add( _order );

        for( TreeFilter tf : children ) {
            tfilters.add( tf.getClass().getSimpleName() );
        }
        return tfilters;
        */
    }

    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        Element root = content.get( _treeKey );
        if( root != null ) {
            Action action = null;
            if( _order == Order.DEPTH_FIRST ) {
                action = TreeWalker.walkDepthFirst( _filter, root );
            }
            else {
                action = TreeWalker.walkBreadthFirst( _filter, root );
            }

            if( action == Action.DROP ) {
                content.remove( _treeKey );
            }
        }

        return true;
    }

    private final Key<Element> _treeKey;
    private final TreeFilter _filter;
    private final Order _order;
}
