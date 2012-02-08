/*
 * Copyright (c) 2012 David Kellum
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

package iudex.html.tree;

import iudex.html.HTML;
import iudex.html.HTMLTag;

import javax.xml.stream.XMLStreamReader;

import com.gravitext.xml.producer.Attribute;
import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.StAXConsumer;

/**
 * StaxConsumer building a Node tree using HTML tags,attributes. Note that
 * error handling is brutal in this implementation. Use the NekoHTMLParser for
 * suspect HTML input.
 */
public class HTMLStAXConsumer extends StAXConsumer
{
    @Override
    protected Element createElement( XMLStreamReader sr )
    {
        HTMLTag tag = HTML.TAGS.get( sr.getLocalName() );
        if( tag == null ) {
            throw new IllegalStateException
                ( "Tag [" + sr.getLocalName() + "] is not a known HTML tag." );

        }
        Element element = new Element( tag );

        return element;
    }

    @Override
    protected Attribute findAttribute( XMLStreamReader sr, int i )
    {
        final Attribute attr =
            HTML.ATTRIBUTES.get( sr.getAttributeLocalName( i ) );

        if( attr == null ) {
            throw new IllegalStateException
            ( "Attribute [" + sr.getAttributeLocalName( i ) +
              "] is not a known HTML attribute." );
        }
        return attr;
    }
}
