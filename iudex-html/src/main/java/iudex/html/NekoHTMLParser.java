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

package iudex.html;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import iudex.core.ContentSource;

import org.cyberneko.html.parsers.SAXParser;
import org.xml.sax.SAXException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import com.gravitext.xml.NamespaceCache;
import com.gravitext.xml.producer.Attribute;
import com.gravitext.xml.tree.AttributeValue;
import com.gravitext.xml.tree.Characters;
import com.gravitext.xml.tree.Element;

public class NekoHTMLParser
{
    public Element parse( ContentSource content )
        throws SAXException, IOException
    {
        Element root = null;
        try {
             root = parse( content, content.defaultEncoding() );
        }
        catch( WrongEncoding wenc ) {
            root = parse( content, wenc.newEncoding() );
        }
        return root;
    }

    private Element parse( ContentSource content, Charset encoding )
        throws SAXException, IOException
    {
        SAXParser parser = new SAXParser();

        // http://nekohtml.sourceforge.net/settings.html
        try {
            parser.setProperty(
        "http://cyberneko.org/html/properties/default-encoding",
                                encoding );
            parser.setFeature(
        "http://cyberneko.org/html/features/scanner/ignore-specified-charset",
                               true );
            parser.setProperty(
        "http://cyberneko.org/html/properties/names/elems",
                                "lower" );
        }
        // SAXNotRecognizedException, SAXNotSupportedException
        catch( SAXException e ) {
            throw new RuntimeException( e );
        }

        HTMLHandler handler = new HTMLHandler();

        parser.setContentHandler( handler );
        parser.parse( new InputSource( content.stream() ) );

        return handler.root();
    }

    static final class WrongEncoding
        extends RuntimeException
    {
        private WrongEncoding( Charset newEncoding )
        {
            _newEncoding = newEncoding;
        }
        public Charset newEncoding()
        {
            return _newEncoding;
        }
        private final Charset _newEncoding;
    }

    static final class HTMLHandler
        extends DefaultHandler
    {
        /**
         * The root Element available after SAX parsing events have been
         * received.
         */
        public Element root()
        {
            return _root;
        }

        @Override
        public void startElement( String iri, String localName, String qName,
                                  Attributes attributes )
        {
            bufferToChars();

            Element element = new Element( _cache.tag( localName, null ) );

            copyAttributes( attributes, element );

            if( _root == null ) {
                _root = _current = element;
            }
            else {
                _current.addChild( element );
                _current = element;
            }
        }

        @Override
        public void endElement( String iri, String localName, String qName )
        {
            bufferToChars();
            _current = _current.parent();
        }

        @Override
        public void characters( char[] ch, int start, int length )
        {
            if( _buffer == null ) {
                _buffer = new StringBuilder( length + 16 );
            }
            _buffer.append( ch, start, length );
        }

        private void bufferToChars()
        {
            if( _buffer != null ) {
                _current.addChild( new Characters( _buffer ) );
                _buffer = null;
            }
        }

        private void copyAttributes( Attributes attributes, Element element )
        {
            final int end = attributes.getLength();
            if( end == 0 ) return;

            final ArrayList<AttributeValue> atts
                = new ArrayList<AttributeValue>( end );

            for( int i = 0; i < end; ++i ) {
                final Attribute attr =
                    _cache.attribute( attributes.getLocalName( i ), null );
                atts.add( new AttributeValue( attr,
                                              attributes.getValue( i ) ) );
            }

            element.setAttributes( atts );
        }

        private Element _root = null;
        private Element _current = null;
        private final NamespaceCache _cache = new NamespaceCache();
        private StringBuilder _buffer = null;
    }
}
