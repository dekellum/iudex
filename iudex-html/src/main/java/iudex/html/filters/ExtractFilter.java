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

import java.util.ArrayList;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;
import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.Node;

import iudex.core.ContentKeys;
import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.filter.FilterException;
import iudex.html.tree.TreeFilter;
import iudex.html.tree.TreeWalker;
import static iudex.html.tree.HTMLTreeKeys.*;

import static iudex.html.HTMLTag.*;

public class ExtractFilter implements Filter, Described
{
    public ExtractFilter( List< Key<Element> > treeKeys )
    {
        this( treeKeys, ContentKeys.EXTRACT );
    }

    public ExtractFilter( List< Key<Element> > treeKeys,
                          Key<CharSequence> extractKey )
    {
        _treeKeys = treeKeys;
        _extractKey = extractKey;
    }

    @Override
    public List<Key> describe()
    {
        ArrayList<Key> keys = new ArrayList<Key>( _treeKeys );
        keys.add( _extractKey );
        return keys;
    }

    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        for( Key<Element> treeKey : _treeKeys ) {
            Element root = content.get( treeKey );
            if( root != null ) {
                Walker walker = new Walker();
                TreeWalker.walkBreadthFirst( walker, root );
            }
        }

        return true;
    }

    private static final class Walker
        implements TreeFilter
    {
        @Override
        public Action filter( Node node )
        {
            Action action = Action.CONTINUE;

            // Check each block level element.
            final Element elem = node.asElement();
            if( ( elem != null ) && !isInline( elem ) ) {

                // Optimize: if total word count for this block is less than prior,
                // than can safely skip it.

                int foundWords = 0;
                int start = 0;

                List<Node> children = elem.children();
                final int end = children.size();
                for( int i = 0; i < end; ++i ) {

                    Node child = children.get( i );
                    final int words = child.get( WORD_COUNT );

                    Element celm = child.asElement();
                    if( ( celm == null ) || isInline( celm ) ) {
                        foundWords += words;
                    }
                    else {
                        // check if found words reaches minimum.
                        // break or continue searching this block with:
                        foundWords = 0;
                        start = i + 1;

                    }
                }
                // check if found words reaches minimum?

            }
            return action;
        }
    }

    private final List<Key<Element>> _treeKeys;
    private final Key<CharSequence> _extractKey;
}
