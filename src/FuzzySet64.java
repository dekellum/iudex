public final class FuzzySet64
{
    public FuzzySet64( int capacity, int thresholdBits )
    {
        //FIXME: throw on capacity < 1
        _set = new long[ capacity ];
        _thresholdBits = thresholdBits;
    }

    public boolean add( final long key )
    {
        final int end = _length;
        for( int i = 0; i < end; ++i ) {
            if ( fuzzyMatch( _set[i], key ) ) return false;
        }
        //FIXME: test length and auto expand
        _set[ _length++ ] = key;
        return true;
    }

    public boolean fuzzyMatch( final long a, final long b )
    {
        return ( Long.bitCount( a ^ b ) <= _thresholdBits );
    }

    private final long[] _set;
    private final int _thresholdBits;
    private int _length = 0;
}
