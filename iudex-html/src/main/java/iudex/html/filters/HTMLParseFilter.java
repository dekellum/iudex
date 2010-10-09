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

import org.xml.sax.SAXException;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;
import com.gravitext.xml.tree.Element;

import iudex.core.ContentSource;
import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.filter.FilterException;

import iudex.html.NekoHTMLParser;

public class HTMLParseFilter
    extends NekoHTMLParser
    implements Filter, Described
{
    HTMLParseFilter( Key<CharSequence> text,
                     Key<Element> tree )
    {
        super();
        _sourceKey = null;
        _textKey = text;
        _treeKey = tree;

        setParseAsFragment( true );
    }

    HTMLParseFilter( Key<ContentSource> source,
                     Key<CharSequence> text,
                     Key<Element> tree )
    {
        super();

        _sourceKey = source;
        _textKey = text;
        _treeKey = tree;

        setParseAsFragment( false );
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
        ContentSource cs = null;
        if( _sourceKey != null ) {
            cs = content.get( _sourceKey );
        }
        else {
            CharSequence text = content.get( _textKey );
            if( text != null ) {
                cs = new ContentSource( text );
            }
        }

        if( cs != null ) {
            try {
                Element e = parse( cs );
            }
            catch( SAXException x ) {
                throw new FilterException( x );
            }
            catch( IOException x ) {
                throw new FilterException( x );
            }
        }

        return true;
    }

    private final Key<ContentSource> _sourceKey;
    private final Key<CharSequence> _textKey;
    private final Key<Element> _treeKey;
}
