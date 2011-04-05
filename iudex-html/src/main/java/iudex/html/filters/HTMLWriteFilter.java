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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;
import com.gravitext.util.ResizableCharBuffer;
import com.gravitext.xml.producer.Indentor;
import com.gravitext.xml.producer.XMLProducer;
import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.NodeWriter;

import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.html.HTML;

public class HTMLWriteFilter
    implements Filter, Described
{
    //FIXME: Use or support ContentSink equiv. as for out?
    //FIXME: This is really XML generic, accept for XHTML namespace.

    public HTMLWriteFilter( Key<Element> treeKey,
                            Key<CharSequence> outKey )
    {
        _treeKey = treeKey;
        _outKey = outKey;
    }

    @Override
    public List<Key> describe()
    {
        return Arrays.asList( (Key) _treeKey, _outKey );
    }

    public void setIndent( Indentor indent )
    {
        _indent = indent;
    }

    public void setIncludeNamespace( boolean includeNamespace )
    {
        _includeNamespace = includeNamespace;
    }

    public void setCapacity( int capacity )
    {
        _capacity = capacity;
    }

    @Override
    public boolean filter( UniMap content )
    {
        try {
            Element root = content.get( _treeKey );
            if( root != null ) {
                ResizableCharBuffer out = new ResizableCharBuffer( _capacity );

                XMLProducer pd = new XMLProducer( out );
                pd.setIndent( _indent );

                if( ! _includeNamespace ) pd.implyNamespace( HTML.NS_XHTML );

                new NodeWriter( pd ).putTree( root );

                content.set( _outKey, out.flipAsCharBuffer() );
            }
        }
        catch( IOException x ) {
            throw new RuntimeException( x );
        }

        return true;
    }

    private final Key<Element> _treeKey;
    private final Key<CharSequence> _outKey;

    private Indentor _indent = Indentor.COMPRESSED;
    private boolean _includeNamespace = false;
    private int _capacity = 2048;
}
