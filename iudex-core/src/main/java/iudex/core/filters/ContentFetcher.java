/*
 * Copyright (c) 2008-2010 David Kellum
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

package iudex.core.filters;

import static iudex.core.ContentKeys.*;
import static iudex.http.HTTPKeys.*;

import iudex.core.ContentSource;
import iudex.core.VisitURL;
import iudex.core.VisitURL.SyntaxException;
import iudex.filter.AsyncFilterContainer;
import iudex.filter.FilterContainer;
import iudex.http.BaseResponseHandler;
import iudex.http.ContentType;
import iudex.http.HTTPClient;
import iudex.http.HTTPSession;
import iudex.http.Header;
import iudex.http.Headers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Charsets;
import com.gravitext.util.ResizableByteBuffer;

public class ContentFetcher implements AsyncFilterContainer
{
    public ContentFetcher( HTTPClient client, FilterContainer receiver )
    {
        _client = client;
        _receiver = receiver;
    }

    /**
     * Set the set of accepted mime types.
     * Types should be normalized: trimmed, lower case, i.e. "text/html" ).
     */
    public void setAcceptedContentTypes( Collection<String> types )
    {
        _acceptedContentTypes = new HashSet<String>( types );
    }

    public void setRequestHeaders( List<Header> headers )
    {
        _fixedRequestHeaders = headers;
    }

    public Set<String> acceptedContentTypes()
    {
        return _acceptedContentTypes;
    }

    public void setDefaultEncoding( Charset defaultEncoding )
    {
        _defaultEncoding = defaultEncoding;
    }

    public boolean filter( UniMap content )
    {
        HTTPSession session = _client.createSession();
        session.setMethod( HTTPSession.Method.GET );
        session.setUrl( content.get( URL ).toString() );
        //FIXME: Abort if no URL?

        CharSequence etag = content.get( ETAG );
        if( etag != null ) {
            session.addRequestHeader( new Header( "If-None-Match", etag ) );
        }

        //Add If-Modified-Since (from LAST_VISIT) in RFC 822 format
        Date lastVisit = content.get( LAST_VISIT );
        if( lastVisit != null ) {
            session.addRequestHeader(
                Headers.createDateHeader( "If-Modified-Since", lastVisit ) );
        }
        // FIXME: Use recorded LAST_SUCCESS_VISIT
        // (i.e. last actual fetch for lastVisit )or Last-Modified from
        // last success stored?

        session.addRequestHeaders( _fixedRequestHeaders );

        _client.request( session, new Handler( content ) );

        return true;
    }

    @Override
    public List<FilterContainer> children()
    {
        return Arrays.asList( _receiver );
    }

    @Override
    public void close()
    {
        _receiver.close();
    }

    private final class Handler extends BaseResponseHandler
    {
        public Handler( UniMap content )
        {
            _content = content;
        }

        @Override
        protected void handleSuccessUnsafe( HTTPSession session )
        {
            setHTTPValues( session );
            _content.set( STATUS, session.responseCode() );

            try {
                handleRedirect( session );
            }
            catch ( VisitURL.SyntaxException e ) {
                safeAbort( session );
                handleError( session, -30 );
            }

            ContentType ctype = Headers.contentType( session.responseHeaders());

            if( !testContentType( ctype ) ) {
                safeAbort( session );
                handleError( session, -20 );
                return;
            }

            List<Header> headers = _content.get( RESPONSE_HEADERS );

            Header etag = Headers.getFirst( headers, "ETag" );
            if( etag != null ) {
                _content.set( ETAG, Headers.asCharSequence( etag.value() ) );
            }

            int len = Headers.contentLength( headers );
            if( len != 0 ) {
                if( len > _maxContentLength ){
                    safeAbort( session );
                    handleError( session, -10 );
                    return;
                }

                ResizableByteBuffer buffer
                    = new ResizableByteBuffer( (len > 0) ? len : 16 * 1024 );
                try {
                    buffer.putFromStream( session.responseStream(),
                                          _maxContentLength + 1, 8 * 1024 );
                }
                catch( IOException x ) {
                    safeAbort( session );
                    handleError( session, -40 );
                    return;
                }

                if( buffer.position() > _maxContentLength ) {
                    safeAbort( session );
                    handleError( session, -10 );
                    return;
                }

                ContentSource cs =
                    new ContentSource( buffer.flipAsByteBuffer() );

                // Set default encoding
                Charset encoding = null;
                if( ctype != null ) {
                    String eName = ctype.charset();
                    if( eName != null ) {
                        encoding = Charsets.lookup( eName );
                    }
                }
                if( encoding == null ) encoding = _defaultEncoding;

                cs.setDefaultEncoding( encoding );

                _content.set( CONTENT, cs );
            }
            _receiver.filter( _content );
        }

        @Override
        public void handleError( HTTPSession session, int code )
        {
            _content.set( STATUS, code );
            setHTTPValues( session );
            super.handleError( session, code );
            _receiver.filter( _content );
        }

        @Override
        public void handleException( HTTPSession session, Exception x )
        {
            _content.set( STATUS, -1 );
            setHTTPValues( session );
            _content.set( REASON, "i.c.f.ContentFetcher: " + x.toString() );
            super.handleException( session, x );
            _receiver.filter( _content );
        }

        private void handleRedirect( HTTPSession session )
            throws SyntaxException
        {
            if( ! _content.get( URL ).toString().equals( session.url() ) ) {
                VisitURL newUrl = VisitURL.normalize( session.url() );
                if( ! newUrl.equals( _content.get( URL ) ) ) {
                    UniMap referer = _content.clone();

                    //FIXME: Session might support obtaining the original
                    //redirect code received. But for now we fake it:
                    referer.set( STATUS, 302 );

                    // FIXME: Avoid circular reference for reference, and
                    // stack overflow on toString, by just making a copy
                    // with URL.
                    UniMap referent = new UniMap();
                    referent.set( URL, newUrl );
                    referer.set( REFERENT, referent );

                    _content.set( REFERER, referer );
                    _content.set( URL, newUrl );
                }
            }
        }

        private void safeAbort( HTTPSession session )
        {
            try {
                session.abort();
            }
            catch( IOException e ) {
                _log.warn(  "On abort (ignored): ", e );
            }
        }

        private boolean testContentType( ContentType ctype )
        {
            boolean match = false;

            if( _acceptedContentTypes != null ) {
                if ( ctype != null ) {
                    String mtype = ctype.type();

                    if ( mtype != null ) {

                        int i = mtype.lastIndexOf( '/' );
                        if( i > 0 ) {
                            String wcard = mtype.subSequence( 0, i ) + "/*";
                            match = _acceptedContentTypes.contains( wcard );
                        }

                        if( !match ) {
                            match = _acceptedContentTypes.contains( mtype );
                        }
                    }
                }
                else {
                    match = _acceptedContentTypes.contains( "*/*" );
                }
            }
            else {
                match = true;
            }

            return match;
        }
        private void setHTTPValues( HTTPSession session )
        {
            _content.set( REQUEST_HEADERS, session.requestHeaders() );
            _content.set( RESPONSE_HEADERS, session.responseHeaders() );
        }

        private final UniMap _content;
    }

    private final int _maxContentLength = 1024 * 1024 - 1;

    private final HTTPClient _client;
    private final FilterContainer _receiver;
    private List<Header> _fixedRequestHeaders = Collections.emptyList();
    private Set<String> _acceptedContentTypes = null;
    private Charset _defaultEncoding = Charset.forName( "ISO-8859-1" );

    private final Logger _log = LoggerFactory.getLogger( ContentFetcher.class );
}
