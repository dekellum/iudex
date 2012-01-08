/*
 * Copyright (c) 2008-2012 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package iudex.core;

import static com.gravitext.util.Charsets.UTF_8;
import iudex.util.Characters;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.gravitext.util.URL64;

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

    /**
     * Return a partially complete, uhash-only VisitURL.
     */
    public static VisitURL fromHash( String hash )
    {
        if( hash == null ) {
            throw new NullPointerException( "fromHash( null )" );
        }
        return new VisitURL( null, hash );
    }

    /**
     * Return VisitURL from safeURL of trusted source and without normalizing.
     */
    public static VisitURL trust( CharSequence safeURL )
    {
        try {
            return new VisitURL( new URI( safeURL.toString() ) );
        }
        catch( URISyntaxException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Return VisitURL from normalizing rawURL as from an untrusted source
     * (i.e. the web).
     * @throws SyntaxException if rawURL can not be salvaged/parsed as a URL.
     */
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

    public boolean hasUrl()
    {
        return ( _uri != null );
    }

    public String url()
    {
        if( ! hasUrl() ) {
            throw new RuntimeException( "url() on VisitURL with only uhash " +
                                        uhash() );
        }
        return _uri.toString();
    }

    public String host()
    {
        if( ! hasUrl() ) {
            throw new RuntimeException( "host() on VisitURL with only uhash " +
                                        uhash() );
        }

        return _uri.getHost();
    }

    /**
     * Returns the registration-level domain for this URL or lacking a
     * registration match, the host name itself.
     * @see Domains#registrationLevelDomain(String)
     */
    public String domain()
    {
        if( _domain == null ) {
            final String h = host();
            final String d = Domains.registrationLevelDomain( h );
            _domain = ( d != null ) ? d : h;
        }
        return _domain;
    }

    public String uhash()
    {
        //lazy init
        if( _uhash == null ) _uhash = hashURL( url() ).toString();
        return _uhash;
    }

    public VisitURL resolve( CharSequence other ) throws SyntaxException
    {
        try {
            URI uri = normalize( _uri.resolve( preEncode( other ) ) );
            return new VisitURL( uri );
        }
        catch( IllegalArgumentException x ) {
            if( x.getCause() != null ) {
                throw new SyntaxException( x.getCause() );
            }
            else {
                throw new SyntaxException( x );
            }
        }
        catch( URISyntaxException x ) {
            throw new SyntaxException( x );
        }
    }

    public int compareTo( VisitURL other )
    {
        return uhash().compareTo( other.uhash() );
    }

    @Override
    public boolean equals( Object other )
    {
        return ( ( other instanceof VisitURL ) &&
            uhash().equals( ((VisitURL) other).uhash() ) );
    }

    @Override
    public int hashCode()
    {
        return uhash().hashCode();
    }

    public String toString()
    {
        return ( hasUrl() ? _uri.toString() : uhash() );
    }

    VisitURL( URI preParsed )
    {
        _uri = preParsed;
    }

    VisitURL( URI preParsed, String hash )
    {
        _uri = preParsed;
        _uhash = hash;
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
        if( host != null ) host = Domains.normalize( host );

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
     * Trim leading/trailing isCtrlWS().
     * Replace internal whitespace sequences with a single %20
     */
    static String preEncode( CharSequence in )
    {
        return Characters.replaceCtrlWS( in, "%20" ).toString();
    }

    private static byte[] hash( CharSequence input )
    {
        try {
            MessageDigest md = MessageDigest.getInstance( "SHA-1" );
            md.update( UTF_8.encode( CharBuffer.wrap( input ) ) );
            return md.digest();

        }
        catch( NoSuchAlgorithmException x ) {
            throw new RuntimeException( x ); //SHA-1 should be available
        }
    }

    private final URI _uri;
    private String _uhash = null;
    private String _domain = null;
}
