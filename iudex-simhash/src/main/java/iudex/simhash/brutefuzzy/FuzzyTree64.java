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
 * A custom search tree implementation of FuzzySet64. The tree
 * exploits the property that, if given some threshold bits T, if we
 * break keys up into T+1 segments, then if a match exists, at least
 * one of these segments must be an exact match.  4, 8 or 16 segments
 * with 2^segments FuzzyList64 lists per segment is used for the search
 * tree.
 */
public final class FuzzyTree64
    implements FuzzySet64
{
    public static final int MAX_THRESHOLD = 15;

    public FuzzyTree64( int capacity, int thresholdBits )
    {
        this( capacity, thresholdBits, 8 );
    }

    public FuzzyTree64( int capacity, int thresholdBits, int maxSegmentBits )
    {
        _thresholdBits = thresholdBits;
        _segmentBits = segmentBits( thresholdBits, maxSegmentBits );
        _segments = 64 / _segmentBits;
        _mask = ( 1L << _segmentBits ) - 1;

        // Initial capacity for leaf nodes
        _listCap = capacity / ( 1 << _segmentBits );

        _tree = setupIndex();
    }

    public boolean addIfNotFound( final long key )
    {
        // Use int segs to avoid sign complications with <= 16 bit segments.
        final int[] segs = new int[ _segments ];
        final FuzzyList64[] lists = new FuzzyList64[ _segments ];
        long skey = key;

        // Search for matches within each segment
        for( int s = 0; s < _segments; ++s ) {
            segs[s] = (int) ( skey & _mask );
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
                _tree[s][ segs[s] ] = lists[s];
            }
            lists[s].store( key );
        }

        return true;
    }

    public boolean findAll( final long key, Collection<Long> matches )
    {
        long skey = key;
        boolean exactMatch = false;

        // Find all matches within each segment
        for( int s = 0; s < _segments; ++s ) {
            int seg = (int) ( skey & _mask );
            FuzzyList64 list = _tree[s][ seg ];
            if( list != null ) {
                if( list.findAll( key, matches ) ) exactMatch = true;
            }
            skey >>>= _segmentBits;
        }

        return exactMatch;
    }

    public boolean addFindAll( final long key, Collection<Long> matches )
    {
        long skey = key;
        boolean exactMatch = false;

        // Search for matches and add new key to each segment
        for( int s = 0; s < _segments; ++s ) {
            int seg = (int) ( skey & _mask );
            FuzzyList64 list = _tree[s][ seg ];
            if( list == null ) {
                list = new FuzzyList64( _listCap, _thresholdBits );
                list.store( key );
                _tree[s][ seg ] = list;
            }
            else {
                if( list.addFindAll( key, matches ) ) exactMatch = true;
            }
            skey >>>= _segmentBits;
        }

        return exactMatch;
    }

    public boolean remove( final long key )
    {
        long skey = key;
        boolean found = false;

        // Remove from all matching segments
        for( int s = 0; s < _segments; ++s ) {
            int seg = (int) ( skey & _mask );
            FuzzyList64 list = _tree[s][ seg ];
            if( list != null ) {
                if( list.remove( key ) ) found = true;
            }
            skey >>>= _segmentBits;
        }

        return found;
    }

    private int segmentBits( int thresholdBits, int maxBits )
    {
        for( int l : SEG_BIT_LENGTHS ) {
            if( ( l <= maxBits ) && ( 64 / l ) > thresholdBits ) return l;
        }
        throw new IllegalArgumentException( "threshold too long" );
    }

    private FuzzyList64[][] setupIndex()
    {
        FuzzyList64[][] index = new FuzzyList64[_segments][];
        for( int s = 0; s < _segments; ++s ) {
            index[s] = new FuzzyList64[ 1 << _segmentBits ];
        }

        return index;
    }

    private final int _listCap;

    private final FuzzyList64[][] _tree;

    private static final int[] SEG_BIT_LENGTHS = { 16, 8, 4 };

    private final int _thresholdBits;
    private final int _segmentBits;
    private final int _segments;
    private final long _mask;
}
