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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;
import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.Node;

import iudex.core.ContentSource;
import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.filter.FilterException;

import iudex.html.NekoHTMLParser;

/**
 * An HTML parsing filter. Supports complete HTML documents or fragments with
 * repeated parsing, for cases of multiply-encoded HTML.
 */
public class HTMLParseFilter
    extends NekoHTMLParser
    implements Filter, Described
{
    /**
     * Parse on text input key to tree output key.
     * Assumes setParseAsFragment( true ).
     * @throws NullPointerException if text or tree keys are null.
     */
    public HTMLParseFilter( Key<CharSequence> text,
                            Key<Element> tree )
    {
        super();

        if( text == null ) {
            throw new NullPointerException( "text key needed" );
        }

        if( tree == null ) {
            throw new NullPointerException( "tree key needed" );
        }

        _sourceKey = null;
        _textKey = text;
        _treeKey = tree;

        setParseAsFragment( true );
    }

    /**
     * Parse on source input key to text (if flat) and tree output keys. Text
     * or tree keys may be omitted (null), but one must be specified.
     * Assumes setParseAsFragment( false ).
     * @throws NullPointerException if source is null or if BOTH text and tree
     *         output keys are null.
     */
    public HTMLParseFilter( Key<ContentSource> source,
                            Key<CharSequence> text,
                            Key<Element> tree )
    {
        super();

        if( source == null ) {
            throw new NullPointerException( "source key needed" );
        }
        if( ( text == null ) && ( tree == null ) ) {
            throw new NullPointerException( "one of text or tree keys needed" );
        }

        _sourceKey = source;
        _textKey = text;
        _treeKey = tree;

        setParseAsFragment( false );
    }

    /**
     * Set default minimum parse iterations (default 1). After the minimum,
     * parses will only be performed if the text looks to be marked up. Zero
     * is a legal minimum.
     */
    public void setMinParse( int minParse )
    {
        _minParse = minParse;
    }

    /**
     * Set maximum number of parse iterations (default 3).
     */
    public void setMaxParse( int maxParse )
    {
        _maxParse = maxParse;
    }

    @Override
    public List<?> describe()
    {
        List<Key> keys = new ArrayList<Key>();

        if( _sourceKey != null ) keys.add( _sourceKey );
        if( _textKey != null )   keys.add( _textKey );
        if( _treeKey != null )   keys.add( _treeKey );

        return keys;
    }

    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        parseLoop( content );
        return true;
    }

    int parseLoop( UniMap content ) throws FilterException
    {
        ContentSource source = null;
        CharSequence text = null;
        if( _sourceKey != null ) {
            source = content.get( _sourceKey );
        }
        else {
            text = content.get( _textKey );
        }
        Element root = null;
        int parses = 0;
        while( parses < _maxParse ) {
            if( source == null ) {
                if( ( text != null ) &&
                    ( ( parses < _minParse ) || textMarked( text ) ) ) {
                    source = new ContentSource( text );
                }
                else break;
            }

            root = parseSafe( source );
            source = null;
            ++parses;

            final CharSequence old = text;
            text = textIfFlat( root );

            // Stop now if not flat or no change in text.
            if( ( text == null ) || equals( old, text ) ) break;
        }

        if( _textKey != null ) content.set( _textKey, text ); //or remove
        if( _treeKey != null ) content.set( _treeKey, root ); //or remove

        //FIXME: If fragment, drop outer div if one child element?

        return parses;
    }

    boolean textMarked( CharSequence text )
    {
        return MARKUP_PATTERN.matcher( text ).find();
    }

    private boolean equals( CharSequence t1, CharSequence t2 )
    {
        //FIXME: CharSequence utilities?

        if( t1 == t2 ) return true;
        if( ( t1 == null ) || ( t2 == null ) ) return false;

        final int len1 = t1.length();
        if( len1 != t2.length() ) return false;

        for( int i = 0; i < len1; ++i ) {
            if( t1.charAt( i ) != t2.charAt( i ) ) return false;
        }
        return true;
    }

    private CharSequence textIfFlat( Element root )
    {
        List<Node> children = root.children();
        if( children.size() == 1 ) {
            Node node = children.get( 0 );
            if( node.isCharacters() ) {
                return node.characters();
            }
        }
        else if( children.size() == 0 ) {
            return ""; //Empty is the new flat.
        }
        return null;
    }

    private Element parseSafe( ContentSource cs ) throws FilterException
    {
        try {
            return parse( cs );
        }
        catch( SAXException x ) {
            throw new FilterException( x );
        }
        catch( IOException x ) {
            throw new FilterException( x );
        }
    }

    private final Key<ContentSource> _sourceKey;
    private final Key<CharSequence>  _textKey;
    private final Key<Element>       _treeKey;

    private int _minParse = 1;
    private int _maxParse = 3;

    // http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
    // Character (Entity) References are from 2-8 chars in length, plus 2 for
    // '&' and ';'. (Max 12 for margin).
    private static final Pattern MARKUP_PATTERN =
        Pattern.compile( "((&[#0-9a-zA-Z]{2,12};)|" +
                          "(<!\\[CDATA\\[)|" +
                          "(<!--)|" +
                          "(<[^>]{1,256}>))" );
}
