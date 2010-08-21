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

import java.util.HashMap;
import java.util.Map;

public final class FuzzyTree64
    implements FuzzySet64
{
    public static final int MAX_THRESHOLD = 15;

    public FuzzyTree64( int capacity, int thresholdBits )
    {
        _capacity = capacity;
        _thresholdBits = thresholdBits;
        _segmentBits = segmentBits( thresholdBits );
        long m = 2;
        for( int i = 0; i < _segmentBits; ++i ) m *= 2;
        _mask = m - 1;
        _index = new HashMap<Integer,FuzzyList64>( _capacity );
    }

    public boolean add( final long key )
    {
        long skey = key;
        int scnt = 64 / _segmentBits;
        Object[] segs = new Object[scnt];

        for( int s = 0; s < scnt; ++s ) {
            segs[s] = new Integer( (int) ( skey & _mask ) );
            FuzzyList64 list = _index.get( segs[s] );
            if( list != null ) {
                if( list.find( key ) ) return false; //found
                segs[s] = list;  //save for possible later set.
            }
            skey >>>= _segmentBits;
        }

        // Not found, insert
        for( int s = 0; s < scnt; ++s ) {
            Object seg = segs[s];
            if( seg instanceof FuzzyList64 ) {
                ( (FuzzyList64) seg ).store( key );
            }
            else { // Integer segment
                FuzzyList64 list =
                    new FuzzyList64( _capacity / _segmentBits, _thresholdBits );
                list.store( key );
                _index.put( (Integer) seg, list );
            }
        }

        return true;
    }

    private int segmentBits( int thresholdBits )
    {
        for( int l : SEG_LENGTHS ) {
            if( (64/l) > thresholdBits ) return l;
        }
        throw new IllegalArgumentException( "threshold too long" );
    }

    private final int _capacity;

    private final Map<Integer,FuzzyList64> _index;

    private static final int[] SEG_LENGTHS = { 32, 16, 8, 4 };

    private final int _thresholdBits;
    private final int _segmentBits;
    private final long _mask;
}
