package iudex.core;

import iudex.util.URL64;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Immutable URL representation encapsulates URL/URI parsing, normalization, 
 * and hashing.
 */
public final class VisitURL
    implements Comparable<VisitURL>
{

    public static class SyntaxException extends IOException
    {
        public SyntaxException( Throwable cause )
        {
            super( cause );
        }
        
        private static final long serialVersionUID = 1L;
    }

    public static VisitURL trust( CharSequence safeURL )
    {
        try {
            return new VisitURL( new URI( safeURL.toString() ) );
        }
        catch( URISyntaxException e ) {
            throw new RuntimeException( e );
        }
    }
    
    public static VisitURL normalize( CharSequence rawURL )
        throws SyntaxException 
    {
        try {
            String raw = preEncode( rawURL );
            return new VisitURL( normalize( new URI( raw ) ) );
        }
        catch( URISyntaxException x ) {
            throw new SyntaxException( x );
        }
    }

    // FIXME: domain() via http://publicsuffix.org/list/ (see notes)
    
    public int compareTo( VisitURL other )
    {
        return _uri.compareTo( other._uri );
    }

    public boolean equals( Object other )
    {
        return ( ( other instanceof VisitURL ) && 
                 _uri.equals( ((VisitURL) other)._uri ) );  
    }

    public String host()
    {
        return _uri.getHost();
    }
    
    public String uhash()
    {
        //lazy init
        if( _uhash == null ) _uhash = hashURL( toString() ).toString(); 
        return _uhash;
    }
    
    public int hashCode()
    {
        return _uri.hashCode();
    }

    public String toString()
    {
        return _uri.toString();
    }
    
    VisitURL( URI preParsed )
    {
        _uri = preParsed;
    }

    /**
     * Return a 23-character URL64 encoded hash of the complete URL.
     */
    static CharSequence hashURL( CharSequence url )
    {
        byte[] sha = hash( url );
        char[] chars = URL64.encode( sha, 0, 18 );
        CharBuffer cbuff = CharBuffer.wrap( chars );
        cbuff.limit( 23 );
        return cbuff;
    }
    
    /**
     * Return 6-character URL64 encoded hash of a domain name.
     */
    static CharSequence hashDomain( CharSequence domain )
    {
        byte[] sha = hash( domain );
        char[] chars = URL64.encode( sha, 0, 5 );
        CharBuffer cbuff = CharBuffer.wrap( chars );
        cbuff.limit( 6 );
        return cbuff;
    }
   
    
    static URI normalize( URI uri ) throws URISyntaxException
    {
        //FIXME: See also http://en.wikipedia.org/wiki/URL_normalization

        uri = uri.parseServerAuthority();

        // FIXME: Use java.net.IDN (for unicode to ascii host conversion)?
        
        // Lower case the scheme
        String scheme = uri.getScheme();
        if( scheme != null ) scheme = scheme.toLowerCase();

        // Lower case the host
        String host = uri.getHost();
        //FIXME: if( host != null ) host = IDN.toASCII( host, 0 );
        if( host != null ) host = host.toLowerCase();
        
        // Drop superfluous port assignments
        int port = uri.getPort();
        if( ( "http".equals(  scheme ) && port ==  80 ) || 
            ( "https".equals( scheme ) && port == 443 ) ) {
            port = -1;
        }
        
        // Drop empty '?' query string.
        String query = uri.getRawQuery();
        if( query != null && query.isEmpty() ) query = null;
        
        // Add '/' with bare http://host -> http://host/.
        String path = uri.getRawPath();
        if( path == null || path.isEmpty() ) path = "/";
        
        StringBuilder b = new StringBuilder();
        b.append( scheme ).append( "://" );
        b.append(  host );
        if( port != -1 ) b.append( ':' ).append( port );
        b.append( path );
        if( query != null ) b.append( '?' ).append( query );
        
        uri = new URI( b.toString() );
        
        /* FIXME: Encode path (good) but encode query is wrong, 
         * so can't use this:
        uri = new URI( scheme,
                       null, // Drop userInfo
                       host,
                       port,
                       path, 
                       query,
                       null ); // Drop fragment (anchor)
        */
        
        uri = new URI( uri.normalize().toASCIIString() );
        
        return uri;
    }

    /**
     * Trim leading trail whitespace
     * Replace internal whitespace sequences with a single %20
     */
    static String preEncode( CharSequence in )
    {
        final StringBuilder clean = new StringBuilder( in.length() );
        int i = 0;
        int last = i;
        boolean inWhitespace = false;
        final int end = in.length();
        while( i < end ) {
            char c = in.charAt(i);
            if( Character.isWhitespace( c ) || Character.isSpaceChar( c ) ) {
                if( !inWhitespace ) {
                    clean.append( in, last, i);
                    inWhitespace = true;
                }
            }
            else if( inWhitespace ) {
                if( clean.length() > 0 ) clean.append( "%20" );
                last = i;
                inWhitespace = false;
            }
            ++i;
        }
        if( !inWhitespace ) clean.append( in, last, i );
        return clean.toString();
    }
    
    
    private static byte[] hash( CharSequence input ) 
    {
        try {
            MessageDigest md = MessageDigest.getInstance( "SHA-1" );
            md.update( UTF8.encode( CharBuffer.wrap( input ) ) );
            return md.digest();
            
        }
        catch( NoSuchAlgorithmException x ) {
            throw new RuntimeException( x ); //SHA-1 should be available
        }
    }
    

    
    
    private final URI _uri;
    private String _uhash = null;
    
    private static final Charset UTF8 = Charset.forName( "UTF-8" );
}
