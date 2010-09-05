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

package brutefuzzy;

import java.util.Random;

public class BruteFuzzy
{
    public static FuzzySet64 createSet64( int capacity, int thresholdBits )
    {
        if( capacity > 228 && thresholdBits <= FuzzyTree64.MAX_THRESHOLD ) {
            return new FuzzyTree64( capacity, thresholdBits );
        }
        else {
            return new FuzzyList64( capacity, thresholdBits );
        }
    }

    public static long unsignedHexToLong( String hex )
    {
        int len = hex.length();
        int sub = Math.min( len, 8 );

        long v  = Long.parseLong( hex.substring( 0,   sub ), 16 );
        v <<= ( len - sub ) * 4;
        if( sub < len ) {
            v  |= Long.parseLong( hex.substring( sub, len ), 16 );
        }

        return v;
    }

    /**
     * Generate a test set of keys.
     */
    public static long[] testKeys( int length, int thresholdBits, int seed )
    {
        Random random = new Random( seed );

        // Start with totally random sample
        long[] keys = new long[length];
        for( int i = 0; i < length; ++i ) {
            keys[i] = random.nextLong();
        }

        // ~5% dup rate in sample, randomly distributed
        int dups = length / 10;
        for( int r = 0; r < dups; ++r ) {
            long k = keys[ random.nextInt(length) ];
            int bitsOff = random.nextInt( thresholdBits * 2 );
            for( int bc = 0; bc < bitsOff; ++bc ) {
                k ^= ( 1L << random.nextInt( 64 ) );
            }
            keys[ random.nextInt(length) ] = k;
        }
        return keys;
    }

}
