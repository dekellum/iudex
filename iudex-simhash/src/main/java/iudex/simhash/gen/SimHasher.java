/*
 * Copyright (c) 2010-2014 David Kellum
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

final class SimHasher
{
    public void addFeature( final long fhash, final int weight )
    {
        long bit = 1L;
        for( int i = 0; i < 64; ++i ) {
            if( ( fhash & bit ) != 0L ) {
                _fvect[ i ] += weight;
            }
            else {
                _fvect[ i ] -= weight;
            }
            bit <<= 1;
        }
    }

    public long summarize()
    {
        long simhash = 0;
        for( int i = 0; i < 64; ++i ) {
            if( _fvect[i] > 0 ) simhash |= 1L;
            simhash <<= 1;
        }
        return simhash;
    }

    private final int[] _fvect = new int[64];
}
