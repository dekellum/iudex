/*
 * Copyright (c) 2011 David Kellum
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

package iudex.chardetector;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Charsets;

import iudex.core.ContentSource;
import iudex.filter.Filter;
import iudex.filter.FilterException;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import static iudex.core.ContentKeys.SOURCE;

public class CharDetectFilter implements Filter
{
    public int maxDetectLength()
    {
        return _maxDetectLength;
    }

    public void setMaxDetectLength( int maxDetectLength )
    {
        _maxDetectLength = maxDetectLength;
    }

    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        ContentSource source = content.get( SOURCE );

        if( source != null ) {
            if( source.source() instanceof ByteBuffer ) {
                byte[] ba = findDetectBuffer( (ByteBuffer) source.source() );
                if( ba != null ) {
                    CharsetDetector detector = new CharsetDetector();
                    detector.setText( ba );

                    Charset defaultEncoding = source.defaultEncoding();
                    if( defaultEncoding != null ) {
                        detector.setDeclaredEncoding( defaultEncoding.name() );
                    }

                    CharsetMatch match = detector.detect();

                    if( match != null ) {
                        Charset cs = Charsets.lookup( match.getName() );
                        if( cs != null ) {
                            source.setDefaultEncoding( cs );
                            source.setEncodingConfidence( match.getConfidence()
                                                          / 100.0F );
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Find the last max-length span of bytes containing the first high-order
     * byte, or null if no high-order bytes.
     */
    byte[] findDetectBuffer( ByteBuffer b )
    {
        byte[] dba = null;
        b = b.slice();

        // Find first/any high-order byte (< 0x9, suggesting UTF-16, or
        // similar). A BOM would similarly be found.
        int i = b.position();
        final int end = b.limit();
        while( i < end ) {
            byte c = b.get( i );
            if( ( c & 0x80 ) != 0 || c < 0x9 ) break;
            ++i;
        }
        if( i < end ) {
            i -= 2;  // back up a bit, given multi-byte potential
            int start = end - _maxDetectLength;
            if( i < start ) start = i;
            if( start < b.position() ) start = b.position();
            b.position( start );

            if( ( b.position() == 0 ) &&
                ( b.remaining() <= _maxDetectLength ) &&
                  b.hasArray() &&
                ! b.isReadOnly() &&
                ( b.arrayOffset() == 0 ) &&
                ( b.array().length == b.limit() ) ) {

                dba = b.array();
            }
            else {
                dba = new byte[ Math.min( b.remaining(), _maxDetectLength ) ];
                b.get( dba );
            }
        }
        return dba;
    }

    private int _maxDetectLength = 8 * 1024;
}
