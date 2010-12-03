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
import com.gravitext.util.ResizableCharBuffer;
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

/**
 * Extracts the first contiguous block of text from a search list of HTML
 * Element trees. Contiguous means no intervening block element. Inline elements
 * will be ignored, i.e. stripped out of the block. This filter depends on the
 * WordCounter having already been applied to tree in the search list.
 */
public class ExtractFilter implements Filter, Described
{
    /**
     * Construct given ordered list of Element trees to search, and default
     * ContentKeys.EXTRACT output key.
     */
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

    /**
     * Set the minimum number of words to extract. Nothing below this
     * word count will be returned. (Default: 4)
     */
    public void setMinWords( int minWords )
    {
        _minWords = minWords;
    }

    /**
     * Set the preferred number of words at which point the search will
     * be terminated. (Default: 8)
     */
    public void setPreferredWords( int minWords )
    {
        _preferredWords = minWords;
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
        final Walker walker = new Walker( _minWords );

        for( Key<Element> treeKey : _treeKeys ) {
            final Element root = content.get( treeKey );
            if( root != null ) {
                TreeWalker.walkBreadthFirst( walker, root );
                if( walker.foundWords() > _preferredWords ) break;
            }
        }

        content.set( _extractKey, walker.extract() );

        return true;
    }

    private final class Walker
        implements TreeFilter
    {
        /**
         * Construct given initial minimum words to find.
         */
        public Walker( int minFindWords )
        {
            _minFindWords = minFindWords;
        }

        @Override
        public Action filter( Node node )
        {
            Action action = Action.CONTINUE;

            // Check each block level element.
            // FIXME: Skip <pre> blocks?

            final Element elem = node.asElement();
            if( ( elem != null ) && !isInline( elem ) ) {

                // Optimization: if total word count for this block is less
                // than minimum, it can be safely skipped.
                if( elem.get( WORD_COUNT ) < _minFindWords ) {
                    action = Action.SKIP;
                }
                else {
                    List<Node> children = elem.children();

                    int rangeWords = 0;
                    int start = 0;
                    final int end = children.size();

                    for( int i = 0; i < end; ++i ) {

                        Node child = children.get( i );

                        Element celm = child.asElement();
                        if( ( celm == null ) || isInline( celm ) ) {
                            rangeWords += child.get( WORD_COUNT );
                        }
                        else {
                            if( rangeWords >= _minFindWords ) {
                                extractRange( rangeWords, children, start, i );
                            }

                            rangeWords = 0;
                            start = i + 1;

                            if( _foundWords >= _preferredWords ) break;
                        }
                    }

                    if( rangeWords >= _minFindWords ) {
                        extractRange( rangeWords, children, start, end );
                    }

                    if( _foundWords >= _preferredWords ) {
                        action = Action.TERMINATE;
                    }
                }
            }
            return action;
        }

        public int foundWords()
        {
            return _foundWords;
        }

        public CharSequence extract()
        {
            return _extract;
        }

        private void extractRange( final int rangeWords,
                                   final List<Node> children,
                                   final int start,
                                   final int end )
        {
            CharSequence first = null;
            ResizableCharBuffer buff = null;

            for( int i = start; i < end; ++i ) {
                CharSequence cc = children.get( i ).characters();
                if( cc != null ) {
                    if( buff != null ) buff.put( cc );
                    else if( first == null ) first = cc;
                    else {
                        buff = new ResizableCharBuffer( first.length() +
                                                        cc.length() + 32 );
                        buff.put( first ).put( cc );
                    }
                }
            }

            _extract = ( buff != null ) ? buff.flipAsCharBuffer() : first;

            _foundWords   = rangeWords;
            _minFindWords = rangeWords + 1;
        }

        private int _minFindWords;
        private int _foundWords = 0;
        private CharSequence _extract = null;
    }

    private final List< Key<Element> > _treeKeys;
    private final Key<CharSequence> _extractKey;

    private int _minWords = 4;
    private int _preferredWords = 8;
}
