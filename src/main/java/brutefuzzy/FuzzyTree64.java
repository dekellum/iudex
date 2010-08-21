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

public final class FuzzyTree64
    implements FuzzySet64
{
    public static final int MAX_THRESHOLD = 15;

    public FuzzyTree64( int capacity, int thresholdBits )
    {
        _thresholdBits = thresholdBits;
        _segmentBits = segmentBits( thresholdBits );
        _segments = 64 / _segmentBits;
        _mask = ( 1 << _segmentBits ) - 1;
        _listCap = Math.max( 2, Math.min( capacity /
                                          ( _segmentBits * 4 ), 64 ) );
        _tree = setupIndex();
    }

    public boolean add( final long key )
    {
        long skey = key;
        short[] segs = new short[_segments];
        FuzzyList64[] lists = new FuzzyList64[_segments];

        // Search for matches within each segment
        for( int s = 0; s < _segments; ++s ) {
            segs[s] = (short) ( skey & _mask );
            lists[s] = _tree[s][ segs[s] ];
            if( lists[s] != null ) {
                if( lists[s].find( key ) ) return false; //found
            }
            skey >>>= _segmentBits;
        }

        // Not found, insert
        for( int s = 0; s < _segments; ++s ) {
            if( lists[s] == null ) {
                lists[s] = new FuzzyList64( _listCap, _thresholdBits );
                _tree[s][ segs[s ] ] = lists[s];
            }
            lists[s].store( key );
        }

        return true;
    }

    private int segmentBits( int thresholdBits )
    {
        for( int l : SEG_BIT_LENGTHS ) {
            if( ( 64 / l ) > thresholdBits ) return l;
        }
        throw new IllegalArgumentException( "threshold too long" );
    }

    private FuzzyList64[][] setupIndex()
    {
        FuzzyList64[][] index =
            new FuzzyList64[_segments][256];
        for( int s = 0; s < _segments; ++s ) {
            index[s] = new FuzzyList64[256];
        }

        return index;
    }

    private final int _listCap;

    private final FuzzyList64[][] _tree;

    private static final int[] SEG_BIT_LENGTHS = { 8, 4 };

    private final int _thresholdBits;
    private final int _segmentBits;
    private final int _segments;
    private final long _mask;
}
