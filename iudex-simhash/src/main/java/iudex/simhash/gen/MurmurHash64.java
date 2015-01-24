/*
 * Copyright (c) 2010-2015 David Kellum
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

/**
 * MurmurHash 64 bit hash utility.
 *
 * This implementation was derived from Aston Appleby's original in C
 * (public domain) and informed by a prior Java port by Viliam Holub
 * (public domain).
 *
 * @see http://murmurhash.googlepages.com/
 * @see http://d3s.mff.cuni.cz/~holub/sw/javamurmurhash/
 * @see http://en.wikipedia.org/wiki/MurmurHash
 */
public final class MurmurHash64
{
    /**
     * Generate a 64 bit hash from CharSequence (as UCS-2, 16-bit characters).
     */
    public static long hash( final CharSequence text )
    {
        return hash( text, 0, text.length() );
    }

    /**
     * Generate a 64 bit hash from bytes with default seed value.
     */
    public static long hash( ByteBuffer b )
    {
        //FIXME: Support native buffers?
        return hash( b.array(), b.arrayOffset() + b.position(), b.remaining() );
    }

    /**
     * Generate a 64 bit hash from byte array with default seed value.
     */
    public static long hash( final byte[] data,
                             final int offset,
                             final int length )
    {
        return hash( data, offset, length, 0xe17a1465 );
    }

    /**
     * Generate a 64 bit hash from data of the given length, with
     * seed.
     */
    public static long hash( final byte[] data,
                             int offset,
                             final int length,
                             final int seed )
    {
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final long m = 0xc6a4a7935bd1e995L;
        final int r = 47;

        long h = ( seed & 0xffffffffL ) ^ ( length * m );

        final int octets = length / 8;
        for( int i = 0; i < octets; ++i ) {

            long k = ( ( ( data[ offset++ ] & 0xffL )       ) |
                       ( ( data[ offset++ ] & 0xffL ) <<  8 ) |
                       ( ( data[ offset++ ] & 0xffL ) << 16 ) |
                       ( ( data[ offset++ ] & 0xffL ) << 24 ) |
                       ( ( data[ offset++ ] & 0xffL ) << 32 ) |
                       ( ( data[ offset++ ] & 0xffL ) << 40 ) |
                       ( ( data[ offset++ ] & 0xffL ) << 48 ) |
                       ( ( data[ offset++ ] & 0xffL ) << 56 ) );

            k *= m;
            k ^= k >>> r;
            k *= m;

            h ^= k;
            h *= m;
        }

        switch( length % 8 ) {
        case 7: h ^= ( data[ offset + 6 ] & 0xffL ) << 48;
        case 6: h ^= ( data[ offset + 5 ] & 0xffL ) << 40;
        case 5: h ^= ( data[ offset + 4 ] & 0xffL ) << 32;
        case 4: h ^= ( data[ offset + 3 ] & 0xffL ) << 24;
        case 3: h ^= ( data[ offset + 2 ] & 0xffL ) << 16;
        case 2: h ^= ( data[ offset + 1 ] & 0xffL ) <<  8;
        case 1: h ^= ( data[ offset     ] & 0xffL );
                h *= m;
        }

        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;

        return h;
    }

    /**
     * Generate a 64 bit hash from the specified CharSequence(as UCS-2) ,
     * with default seed value.
     */
    public static long hash( final CharSequence cs,
                             final int offset,
                             final int length )
    {
        return hash( cs, offset, length, 0xe17a1465 );
    }

    /**
     * Generate a 64 bit hash from CharSequence (as UCS-2), with seed.
     */
    public static long hash( final CharSequence cs,
                             int offset,
                             final int length,
                             final int seed )
    {
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final long m = 0xc6a4a7935bd1e995L;
        final int r = 47;

        long h = ( seed & 0xffffffffL ) ^ ( length * m );

        final int quartets = length / 4;
        for( int i = 0; i < quartets; ++i ) {

            long k = ( ( ( cs.charAt( offset++ ) & 0xffffL )       ) |
                       ( ( cs.charAt( offset++ ) & 0xffffL ) << 16 ) |
                       ( ( cs.charAt( offset++ ) & 0xffffL ) << 32 ) |
                       ( ( cs.charAt( offset++ ) & 0xffffL ) << 48 ) );

            k *= m;
            k ^= k >>> r;
            k *= m;

            h ^= k;
            h *= m;
        }

        switch( length % 4 ) {
        case 3: h ^= ( cs.charAt( offset + 2 ) & 0xffffL ) << 32;
        case 2: h ^= ( cs.charAt( offset + 1 ) & 0xffffL ) << 16;
        case 1: h ^= ( cs.charAt( offset     ) & 0xffffL );
                h *= m;
        }

        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;

        return h;
    }

}
