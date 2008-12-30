package com.gravitext.crawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.gravitext.util.URL64;

public class URLHash
{
    /**
     * Return a 23-character URL64 encoded hash of the complete URL.
     */
    public static CharSequence hashURL( CharSequence url )
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
    public static CharSequence hashDomain( CharSequence domain )
    {
        byte[] sha = hash( domain );
        char[] chars = URL64.encode( sha, 0, 5 );
        CharBuffer cbuff = CharBuffer.wrap( chars );
        cbuff.limit( 6 );
        return cbuff;
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
    
    // FIXME: http://publicsuffix.org/list/ (see notes)
    // * Strip leading/trailing whitespace?
    // * Decode unnecessary encode portions?
    // * Strip any "#anchor"

    public static URI normalize( final URI in ) throws URISyntaxException
    {
        URI uri = in.parseServerAuthority();
        
        // Lower case the scheme
        String scheme = uri.getScheme();
        if( scheme != null ) scheme = scheme.toLowerCase();

        // Lower case the host
        String host = uri.getHost();
        if( host != null ) host = host.toLowerCase();

        // FIXME: Use java.net.IDN (for unicode to ascii domain conversion)?
        
        // Drop superfluous port assignments
        int port = uri.getPort();
        if( ( "http".equals(  scheme ) && port ==  80 ) || 
            ( "https".equals( scheme ) && port == 443 ) ) {
            port = -1;
        }
        
        uri = new URI( scheme,
                       null, //userInfo
                       host,
                       port,
                       uri.getPath(), 
                       uri.getQuery(),
                       null ); // fragment (anchor)
        
        uri = new URI( uri.normalize().toASCIIString() );
        
        return uri;
    }
    
    
    private static final Charset UTF8 = Charset.forName( "UTF-8" );
}
