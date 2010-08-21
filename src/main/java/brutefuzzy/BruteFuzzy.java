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

public class BruteFuzzy
{
    public static FuzzySet64 createSet64( int capacity, int thresholdBits )
    {
        if( capacity > 100 && thresholdBits <= FuzzyTree64.MAX_THRESHOLD ) {
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

}
