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
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.gravitext.util.URL64;

/**
 * Immutable URL representation encapsulates URL/URI parsing,
 * normalization, and hashing.
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

        public SyntaxException( String message )
        {
            super( message );
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
     * Return VisitURL from normalizing rawURL as from an untrusted
     * source (i.e. the web).
     * @throws SyntaxException if rawURL can not be salvaged/parsed as
     *         a valid HTTP URL.
     */
    public static VisitURL normalize( CharSequence rawURL )
        throws SyntaxException
    {
        try {
            String raw = preEncode( rawURL );
            return new VisitURL( normalizeStringURI( raw ) );
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
    
    static URI normalize( URI resolvedUri ) throws URISyntaxException, SyntaxException
    {
        // This inefficiently stringifys a URI only to re-parse. Needed to sort out
        // IDN domains not allowed in java.net.URI
        return normalizeStringURI( resolvedUri.toASCIIString() );
    }

    static URI normalizeStringURI( String rawUri ) throws URISyntaxException, SyntaxException
    {
        //FIXME: See also http://en.wikipedia.org/wiki/URL_normalization
        
        // Use URL instead of URI since it allows IDN
        URL someURL;
        try {
            someURL = new URL( rawUri );
        } catch (MalformedURLException e) {
            throw new URISyntaxException(rawUri, e.getMessage());
        }

        // Lower case the scheme
        String scheme = someURL.getProtocol();
        if( scheme == null ) {
            throw new SyntaxException( "No URI scheme for [" + someURL + "]" );
        }
        scheme = scheme.toLowerCase();
        if( !( "http".equals( scheme ) || "https".equals( scheme ) ) ) {
            throw new SyntaxException( "Non-HTTP scheme [" + someURL + "]" );
        }

        // Lower case and normalize the host
        String host = someURL.getHost();
        // Domains#normalize accepts null and can return null in
        // other cases as well. Check after normalize
        host = Domains.normalize( host );
        if( host == null ) {
            throw new SyntaxException( "No host in [" + someURL + "]" );
        }
        

        int port = someURL.getPort();
        if( port == 0 || port > 65535 ) {
            throw new SyntaxException(
                "Invalid port " + port + " in [" + someURL + "]" );
        }

        // Drop superfluous port assignments
        if( ( "http".equals(  scheme ) && port ==  80 ) ||
            ( "https".equals( scheme ) && port == 443 ) ) {
            port = -1;
        }

        // Drop empty '?' query string.
        String query = someURL.getQuery();
        if( query != null && query.isEmpty() ) query = null;

        // Add '/' with bare http://host -> http://host/.
        String path = someURL.getPath();
        if( path == null || path.isEmpty() ) path = "/";

        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append( scheme ).append( "://" );
        uriBuilder.append(  host );
        if( port != -1 ) uriBuilder.append( ':' ).append( port );
        uriBuilder.append( path );
        if( query != null ) uriBuilder.append( '?' ).append( query );

        URI uri = new URI( uriBuilder.toString() );
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
