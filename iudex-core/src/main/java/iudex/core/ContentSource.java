/*
 * Copyright (C) 2008-2009 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package iudex.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ContentSource
{

    public ContentSource( ByteBuffer buffer )
    {
        _source = buffer;
    }

    public ContentSource( InputStream in )
    {
        _source = in;
    }

    public InputStream stream()
    {
        if( _source instanceof InputStream ) {
            return (InputStream) _source;
        }
        else if( _source instanceof ByteBuffer ) {
            ByteBuffer buf = (ByteBuffer) _source;
            return new ByteArrayInputStream( buf.array(),
                                             buf.arrayOffset() + buf.position(),
                                             buf.remaining() );
            //FIXME: Replace with more optimized (non-synchronized) stream?
        }
        return null;
    }

    public CharSequence characters()
    {
        return ( ( _source instanceof CharSequence ) ?
                 (CharSequence) _source : null );
    }

    public Charset defaultEncoding()
    {
        return _defaultEncoding;
    }

    public Object source()
    {
        return _source;
    }

    private Object _source;
    private Charset _defaultEncoding = null;

}
