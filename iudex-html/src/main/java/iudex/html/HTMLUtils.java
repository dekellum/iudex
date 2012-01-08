/*
 * Copyright (c) 2008-2012 David Kellum
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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.xml.sax.SAXException;

import iudex.core.ContentSource;
import iudex.html.neko.NekoHTMLParser;

import com.gravitext.xml.producer.Indentor;
import com.gravitext.xml.producer.XMLProducer;
import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.Node;
import com.gravitext.xml.tree.NodeWriter;

public class HTMLUtils
{
    public static ContentSource source( byte[] input, String defaultCharset )
    {
        ContentSource cs = new ContentSource( ByteBuffer.wrap( input ) );
        cs.setDefaultEncoding( Charset.forName( defaultCharset ) );
        return cs;
    }

    public static Element parseFragment( ContentSource source )
        throws SAXException, IOException
    {
        NekoHTMLParser parser = new NekoHTMLParser();
        parser.setParseAsFragment( true );
        return parser.parse( source );
    }

    public static Element parse( ContentSource source )
        throws SAXException, IOException
    {
        return new NekoHTMLParser().parse( source );
    }

    public static void produceFragment( Node root,
                                        Indentor indent,
                                        Appendable out )
        throws IOException
    {
        XMLProducer pd = new XMLProducer( out );
        pd.setIndent( indent );
        pd.implyNamespace( HTML.NS_XHTML );
        new NodeWriter( pd ).putTree( root );
    }

    public static String produceFragmentString( Node root, Indentor indent )
        throws IOException
    {
        StringBuilder out = new StringBuilder( 256 );
        produceFragment( root, indent, out );
        return out.toString();
    }
}
