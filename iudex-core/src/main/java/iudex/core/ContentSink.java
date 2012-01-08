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

package iudex.core;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import com.gravitext.util.ResizableCharBuffer;
import com.gravitext.util.ResizableCharBufferWriter;

public final class ContentSink
{
    //FIXME: Implement closeable?

    public ContentSink( OutputStream out, Charset charset )
    {
        _outStream = out;
        _charset = charset;
    }

    public ContentSink( Writer out )
    {
        _outWriter = out;
    }

    /**
     * Construct as in memory character buffer of specified initial capacity.
     */
    public ContentSink( int capacity )
    {
        _outChars = new ResizableCharBuffer( capacity );
    }

    public Appendable appendable()
    {
        if( _outChars != null ) return _outChars;
        return writer();
    }

    public Writer writer()
    {
        if( _outWriter != null ) return _outWriter;
        if( _outStream != null ) new OutputStreamWriter( _outStream, _charset );
        return new ResizableCharBufferWriter( _outChars );
    }

    public CharBuffer bufferedChars()
    {
        if( _outChars == null ) {
            throw new IllegalStateException
                ( "Attempt to access buffer of non buffered ContentSink." );
        }
        return _outChars.flipAsCharBuffer();
        //FIXME: flush() or close() here?
    }

    private ResizableCharBuffer _outChars  = null;
    private OutputStream        _outStream = null;
    private Charset             _charset   = null;
    private Writer              _outWriter = null;
}
