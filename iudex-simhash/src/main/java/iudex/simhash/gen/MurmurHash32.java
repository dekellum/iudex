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

package iudex.simhash.gen;

import java.nio.ByteBuffer;

/**
 * MurmurHash 32 bit hash utility.
 *
 * This implementation was derived from Aston Appleby's original in C
 * (public domain) and informed by a prior Java port by Viliam Holub
 * (public domain).
 *
 * @see http://murmurhash.googlepages.com/
 * @see http://d3s.mff.cuni.cz/~holub/sw/javamurmurhash/
 * @see http://en.wikipedia.org/wiki/MurmurHash
 */
public final class MurmurHash32
{
    /**
     * Generate a 32 bit hash from CharSequence (as UCS-2, 16-bit characters).
     */
    public static int hash( final CharSequence text )
    {
        return hash( text, 0, text.length() );
    }

    /**
     * Generate a 32 bit hash from bytes with default seed value.
     */
    public static int hash( ByteBuffer b )
    {
        //FIXME: Support native buffers?
        return hash( b.array(), b.arrayOffset() + b.position(), b.remaining() );
    }

    /**
     * Generate a 32 bit hash from byte array with default seed value.
     */
    public static int hash( final byte[] data,
                            final int offset,
                            final int length )
    {
        return hash( data, offset, length, 0x9747b28c );
    }

    /**
     * Generate a 32 bit hash from byte data, with seed.
     */
    public static int hash( final byte[] data,
                            int offset,
                            final int length,
                            final int seed )
    {
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final int m = 0x5bd1e995;
        final int r = 24;

        int h = seed ^ length;

        final int quartets = length / 4;
        for (int i = 0; i < quartets; ++i ) {

            int k = ( ( ( data[ offset++ ] & 0xff )       ) |
                      ( ( data[ offset++ ] & 0xff ) <<  8 ) |
                      ( ( data[ offset++ ] & 0xff ) << 16 ) |
                      ( ( data[ offset++ ] & 0xff ) << 24 ) );

            k *= m;
            k ^= k >>> r;
            k *= m;

            h *= m;
            h ^= k;
        }

        // Handle the last few bytes of the input array
        switch( length % 4 ) {
        case 3: h ^= ( data[ offset + 2 ] & 0xff ) << 16;
        case 2: h ^= ( data[ offset + 1 ] & 0xff ) <<  8;
        case 1: h ^= ( data[ offset     ] & 0xff );
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }

    /**
     * Generate a 32 bit hash from CharSequence (as UCS-2), with default
     * seed value.
     */
    public static int hash( final CharSequence cs,
                            final int offset,
                            final int length )
    {
        return hash( cs, offset, length, 0x9747b28c );
    }

    /**
     * Generate a 32 bit hash from CharSequence (as UCS-2), with seed.
     */
    public static int hash( final CharSequence cs,
                            int offset,
                            final int length,
                            final int seed )
    {
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final int m = 0x5bd1e995;
        final int r = 24;

        int h = seed ^ length;

        final int pairs = length / 2;
        for (int i = 0; i < pairs; ++i ) {

            int k = ( ( ( cs.charAt( offset++ ) & 0xffff )       ) |
                      ( ( cs.charAt( offset++ ) & 0xffff ) << 16 ) );

            k *= m;
            k ^= k >>> r;
            k *= m;

            h *= m;
            h ^= k;
        }

        // Handle the last odd char of input
        if( length % 2 == 1 ) {
            h ^= ( cs.charAt( offset ) & 0xffff );
            h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }
}
