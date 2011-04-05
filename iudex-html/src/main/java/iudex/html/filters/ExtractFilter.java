/*
 * Copyright (c) 2010-2011 David Kellum
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
        final Extractor extractor = new Extractor( _minWords );

        for( Key<Element> treeKey : _treeKeys ) {
            final Element root = content.get( treeKey );
            if( root != null ) {
                if( extractor.search( root ) >= _preferredWords ) break;
            }
        }

        content.set( _extractKey, extractor.extract() );

        return true;
    }

    private final class Extractor
    {
        /**
         * Construct given initial minimum words to find.
         */
        public Extractor( int minFindWords )
        {
            _minFindWords = minFindWords;
        }

        public int search( Element elem )
        {
            search( new Range( elem ), elem );
            return foundWords();
        }

        public int foundWords()
        {
            return ( _found != null ) ? _found.words() : 0;
        }

        public CharSequence extract()
        {
            return ( _found != null ) ? _found.extract() : null;
        }

        private void search( Range range, final Element elem )
        {
            final List<Node> children = elem.children();
            final int end = children.size();
            for( int i = 0; i < end; ++i ) {
                final Node cnode = children.get( i );
                final Element celm = cnode.asElement();

                if( celm == null ) { //characters
                    range.add( cnode );
                }
                else if( isInline( celm ) ) {
                    search( range, celm );
                }
                else { //block
                    if( range.words() >= _minFindWords ) {
                        _found = range.terminate( celm );
                        _minFindWords = _found.words() + 1;
                    }
                    if( foundWords() >= _preferredWords ) break;

                    // Optimization: if total word count for this block is less
                    // than minimum, it can be safely skipped.
                    if( celm.get( WORD_COUNT ) >= _minFindWords ) {
                        search( new Range( celm ), celm );
                    }

                    range = new Range( elem, i + 1 );
                }
                if( foundWords() >= _preferredWords ) break;
            }

            if( range.words() >= _minFindWords ) {
                _found = range.terminate( null );
                _minFindWords = _found.words() + 1;
            }
        }

        private int _minFindWords;
        private Range _found = null;
    }

    private static final class Range
    {
        public Range( Element elem )
        {
            this( elem, 0 );
        }

        public Range( Element elem, int index )
        {
            _start = elem;
            _startIndex = index;
        }

        public void add( Node cnode )
        {
            _words += cnode.get( WORD_COUNT );
        }

        public int words()
        {
            return _words;
        }

        public CharSequence extract()
        {
            return _extract;
        }

        public Range terminate( final Element end )
        {
            _extract = null;

            extractRange( _start, _startIndex, end );

            if( _buff != null ) {
                _extract = _buff.flipAsCharBuffer();
                _buff = null;
            }

            return this;
        }

        private void extractRange( final Element parent,
                                   final int start,
                                   final Element end )
        {
            List<Node> children = parent.children();
            final int cend = children.size();
            for( int i = start; i < cend; ++i ) {
                Node cnode = children.get( i );

                Element celm = cnode.asElement();
                if( celm != null ) {
                    if( celm == end ) break;

                    extractRange( celm, 0, end );
                }
                else {
                    CharSequence cc = cnode.characters();
                    if( cc != null ) {
                        if( _buff != null ) _buff.put( cc );
                        else if( _extract == null ) _extract = cc;
                        else {
                            _buff = new ResizableCharBuffer(
                                        _extract.length() + cc.length() + 32 );
                            _buff.put( _extract ).put( cc );
                        }
                    }
                }
            }
        }

        private final Element _start;
        private final int _startIndex;

        private int _words = 0;
        private ResizableCharBuffer _buff = null;
        private CharSequence _extract = null;
    }

    private final List< Key<Element> > _treeKeys;
    private final Key<CharSequence> _extractKey;

    private int _minWords = 4;
    private int _preferredWords = 8;
}
