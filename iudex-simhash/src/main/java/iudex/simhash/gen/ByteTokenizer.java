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

package iudex.simhash.gen;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Tokenize a UTF-8 byte array on occurrences of the SPACE (0x20) byte. This
 * tokenizer expects whitespace/special characters to be normalized prior to
 * use.
 *
 * @see iudex.util.Characters
 */
public class ByteTokenizer
    implements Iterator<ByteBuffer>
{
    public ByteTokenizer( ByteBuffer in )
    {
        _in = in;
    }

    @Override
    public boolean hasNext()
    {
        if( _next == null ) {
            if( _in.hasRemaining() ) {
                scan();
                return ( _next != null );
            }
            else return false;
        }
        return true;
    }

    @Override
    public ByteBuffer next()
    {
        if( hasNext() ) {
            ByteBuffer n = _next;
            _next = null;
            return n;
        }
        throw new IllegalStateException( "next() without hasNext()" );
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException( "ByteTokenizer.remove()" );
    }

    private void scan()
    {
        int pos = _in.position();
        final int end = _in.limit();
        int start = -1;

        while( pos < end ) {
            if( _in.get( pos ) == SPACE ) {
                if( start >= 0 ) break;
            }
            else if( start < 0 ) start = pos;

            ++pos;
        }

        if( start >= 0 && start < pos ) {
            _next = _in.duplicate();
            _next.position( start );
            _next.limit( pos );
            _in.position( ( pos < end ) ? pos + 1 : end );
        }
    }

    private static final byte SPACE = (byte) 0x20;

    private final ByteBuffer _in;
    private ByteBuffer _next = null;
}
