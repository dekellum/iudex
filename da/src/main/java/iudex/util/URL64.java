package iudex.util;

/**
 * URL64 encoder and decoder utility.  URL64 is identical to Base64
 * except that all characters can be safely inserted into a URL
 * paramater without URL encoding.  The 65 character grammar includes:
 * A-Z a-z 0-9 '-' '_' and '.'  The final '.' is used only as a
 * padding character.
 */
public final class URL64 
{

    public final static class FormatException 
        extends Exception 
    {
        public FormatException( String message )
        { 
            super( message ); 
        }
        private static final long serialVersionUID = 1L;
    }

    /**
     * Encode bytes to URL64 characters
     */
    static public char[] encode( final byte[] in )
    {
        return encode( in, 0, in.length );
    }
    

    /**
     * Encode bytes to URL64 characters
     */
    static public char[] encode( final byte[] in, 
                                 final int offset, 
                                 final int length )
    {
    
        char[] out = new char[( (length + 2) / 3) * 4 ];

        // 3 bytes encode to 4 chars.  Output is always an even
        // multiple of 4 characters.
        final int end = offset + length; 
        for( int i = offset, o = 0; i < end; i += 3, o += 4 ) {
            boolean quad = false;
            boolean trip = false;

            int val = (0xFF & (int) in[i]);
            val <<= 8;
            if( (i+1) < length ) {
                val |= (0xFF & (int) in[i+1]);
                trip = true;
            }
            val <<= 8;
            if( (i+2) < length ) {
                val |= (0xFF & (int) in[i+2]);
                quad = true;
            }
            out[ o+3 ] = LEXICON[(quad? (val & 0x3F): 64)];
            val >>= 6;
            out[ o+2 ] = LEXICON[(trip? (val & 0x3F): 64)];
            val >>= 6;
            out[ o+1 ] = LEXICON[val & 0x3F];
            val >>= 6;
            out[ o+0 ] = LEXICON[val & 0x3F];
        }
        return out;
    }

    /**
     * Decode URL64 encoded characters and return original bytes.
     */
    static public byte[] decode( final char[] in ) throws FormatException
    {
        int len = ((in.length + 3) / 4) * 3;
        if (in.length >= 1 && in[in.length - 1] == '.') --len;
        if (in.length >= 2 && in[in.length - 2] == '.') --len;

        final byte[] out = new byte[len];

        int shift = 0;
        int accum = 0;
        int o = 0;
        int value;

        for( int i=0; i < in.length; i++ ) {
            value = OUT_OF_RANGE;
            if( in[i] <= 255 ) value = CODES[ in[i] ];
            if( value == OUT_OF_RANGE ) {
                throw new FormatException
                    ( "Invalid URL64 character '" + in[i] + "'." );
            }
            
            if( value == PAD ) break; // break on padding

            accum <<= 6;
            shift += 6;
            accum |= value;
            if( shift >= 8 ) {
                shift -= 8;
                out[ o++ ] = (byte) ((accum >> shift) & 0xff);
            }
        }

        if( o != out.length ) {
            throw new FormatException
                ( "Invalid URL64 sequence at position " + o + '.' );
        }

        return out;
    }

    private static final int PAD = -1;
    private static final int OUT_OF_RANGE = -2;

    // code characters for values 0..64
    private static final char[] LEXICON =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_."
        .toCharArray();

    // lookup table for converting URL64 characters to value in range 0..63
    private static final byte[] CODES = new byte[256];
    static {
        for (int i=0; i<256; i++) CODES[i] = OUT_OF_RANGE;
        for (int i = 'A'; i <= 'Z'; i++) CODES[i] = (byte)(     i - 'A');
        for (int i = 'a'; i <= 'z'; i++) CODES[i] = (byte)(26 + i - 'a');
        for (int i = '0'; i <= '9'; i++) CODES[i] = (byte)(52 + i - '0');
        CODES['-'] = 62;
        CODES['_'] = 63;
        CODES['.'] = PAD;
    }
}





