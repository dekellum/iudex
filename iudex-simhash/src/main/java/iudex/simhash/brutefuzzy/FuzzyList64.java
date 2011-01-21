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

package iudex.simhash.brutefuzzy;

import java.util.Collection;

/**
 * A linked array, brute-force scan implementation of FuzzySet64.
 */
public final class FuzzyList64
    implements FuzzySet64
{
    public FuzzyList64( int capacity, final int thresholdBits )
    {
        // Use 2, 4, or 8k bytes list segments.
        // The -8 (*8 = 56 bytes) is for size of this + array overhead
        // (see below) to keep complete segment at page boundary size.
             if( capacity > ( 1024 * 3 / 2 ) ) capacity = 1024 - 8;
        else if( capacity > (  512 * 3 / 2 ) ) capacity =  512 - 8;
        else                                   capacity =  256 - 8;

        _set = new long[ capacity ];
        _thresholdBits = thresholdBits;
    }

    public boolean addIfNotFound( final long key )
    {
        final boolean vacant = ! find( key );
        if( vacant ) store( key );
        return vacant;
    }

    public boolean find( final long key )
    {
        final int end = _length;
        for( int i = 0; i < end; ++i ) {
            if ( fuzzyMatch( _set[i], key ) ) return true;
        }
        if( _next != null ) return _next.find( key );
        return false;
    }

    public boolean findAll( final long key, final Collection<Long> matches )
    {
        boolean exactMatch = false;
        final int end = _length;
        for( int i = 0; i < end; ++i ) {
            if ( fuzzyMatch( _set[i], key ) ) {
                matches.add( _set[i] );
                if( _set[i] == key ) exactMatch = true;
            }
        }
        if( _next != null ) {
            if( _next.findAll( key, matches ) ) exactMatch = true;
        }

        return exactMatch;
    }

    public boolean addFindAll( long key, Collection<Long> matches )
    {
        boolean exactMatch = findAll( key, matches );
        if( ! exactMatch ) store( key );
        return exactMatch;
    }

    public boolean remove( final long key )
    {
        boolean found = false;
        final int end = _length;
        for( int i = 0; i < end; ++i ) {
            if ( _set[i] == key ) {
                if( _length - i - 1 > 0 ) {
                    System.arraycopy( _set, i + 1, _set, i, _length - i - 1 );
                }
                --_length;
                found = true;
                break;
            }
        }
        if( !found && ( _next != null ) ) {
            found = _next.remove( key );
        }

        return found;
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
        if( _length < _set.length ) {
            _set[ _length++ ] = key;
        }
        else {
            if( _next == null ) {
                _next = new FuzzyList64( _set.length, _thresholdBits );
            }
            _next.store( key );
        }
    }

    //x86_64 size: (this: 2 * 8 ) + 4 + 8 + 4 + 8 +
    //                            (array: 3*8 ) = 8 * 8 = 64 bytes
    private final int _thresholdBits;
    private final long[] _set;
    private int _length = 0;
    private FuzzyList64 _next = null;
}
