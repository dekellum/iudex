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

package iudex.simhash.brutefuzzy;

/**
 * A brute force array scan implementation of FuzzySet64.
 */
public final class FuzzyList64
    implements FuzzySet64
{
    public FuzzyList64( int initialCapacity, int thresholdBits )
    {
        if( initialCapacity > 0 ) {
            _set = new long[ initialCapacity ];
        }
        _thresholdBits = thresholdBits;
    }

    public boolean add( final long key )
    {
        final boolean vacant = ! find( key );
        if( vacant ) store( key );
        return vacant;
    }

    public boolean find( final long key )
    {
        final int end = _length;
        final long[] set = _set;
        for( int i = 0; i < end; ++i ) {
            if ( fuzzyMatch( set[i], key ) ) return true;
        }
        return false;
    }

    public boolean fuzzyMatch( final long a, final long b )
    {
        final long xor = a ^ b;

        int diff = Integer.bitCount( (int) xor );
        if( diff <= _thresholdBits ) {
            diff +=  Integer.bitCount( (int) ( xor >> 32 ) );
            return ( diff <= _thresholdBits );
        }
        return false;
    }

    void store( final long key )
    {
        checkCapacity();
        _set[ _length++ ] = key;
    }

    private void checkCapacity()
    {
        if( _set.length <= _length ) {
            int size = _set.length;
            size *= 2;
            if( size < 4 ) size = 4;
            long[] snew = new long[ size ];
            System.arraycopy( _set, 0, snew, 0, _length );
            _set = snew;
        }
    }

    private static final long[] EMPTY_SET = {};
    private long[] _set = EMPTY_SET;
    private final int _thresholdBits;
    private int _length = 0;
}
