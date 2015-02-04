/*
 * Copyright (c) 2008-2015 David Kellum
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
package iudex.asynchttpclient;

import iudex.http.ContentType;
import iudex.http.ContentTypeSet;
import iudex.http.HTTPClient;
import iudex.http.HTTPSession;
import iudex.http.Header;
import iudex.http.Headers;
import iudex.http.ResponseHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.util.Closeable;
import com.gravitext.util.ResizableByteBuffer;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;

public class Client implements HTTPClient, Closeable
{
    public Client( AsyncHttpClient client )
    {
        _client = client;
    }

    @Override
    public HTTPSession createSession()
    {
        Session session = new Session();
        session.setMaxContentLength( _maxContentLength );
        session.setAcceptedContentTypes( _acceptedContentTypes );
        return session;
    }

    @Override
    public void request( HTTPSession session, ResponseHandler handler )
    {
        ((Session) session).execute( handler );
    }

    /**
     * Set the set of accepted Content Type patterns.
     */
    public void setAcceptedContentTypes( ContentTypeSet types )
    {
        _acceptedContentTypes = types;
    }

    /**
     * Set maximum length of response body accepted.
     */
    public void setMaxContentLength( int length )
    {
        _maxContentLength = length;
    }

    @Override
    public void close()
    {
        _client.close();
    }

    private class Session
        extends HTTPSession
        implements AsyncHandler<Session>
    {
        public void addRequestHeader( Header header )
        {
            _requestedHeaders.add( header );
        }

        public List<Header> requestHeaders()
        {
            if( _request != null ) {

                FluentCaseInsensitiveStringsMap inHeaders =
                    _request.getHeaders();

                List<Header> outHeaders =
                    new ArrayList<Header>( _implHeaders.size() +
                                           inHeaders.size() );

                outHeaders.addAll( _implHeaders );

                copyHeaders( inHeaders, outHeaders );

                return outHeaders;
            }
            return _requestedHeaders;
        }

        @Override
        public int statusCode()
        {
            return _statusCode;
        }

        public String statusText()
        {
            return _statusText;
        }

        public List<Header> responseHeaders()
        {
            if( _responseHeaders != null ) {
                return _responseHeaders;
            }

            return Collections.emptyList();
        }

        public ByteBuffer responseBody()
        {
            if ( _body != null ) {
                return _body.flipAsByteBuffer();
            }
            return null;
        }

        public void abort()
        {
            if( _state != STATE.ABORT ) {
                if( _future != null ) {
                    _future.abort( new AbortedException() );
                }
                _state = STATE.ABORT;
            }
        }

        public void close()
        {
            //No-op
        }

        void execute( ResponseHandler handler )
        {
            _handler = handler;

            BoundRequestBuilder rb = null;

            switch( method() ) {
            case GET:
                rb = _client.prepareGet( url() );
                break;
            case HEAD:
                rb = _client.prepareHead( url() );
                break;
            case POST:
                rb = _client.preparePost( url() );
                if( requestContent() == null ) {
                    throw new IllegalArgumentException(
                        "HTTPSession.requestContent required for POST method" );
                }
                ByteBuffer b = requestContent().content();
                // FIXME: Sad copy due to unsupported ByteBuffer
                byte[] bb = new byte[ b.remaining() ];
                b.get( bb );
                rb.setBody( bb );
                rb.setHeader( "Content-Type", requestContent().type() );
                break;
            default:
                throw new UnsupportedOperationException(
                    "Unsupported Method." + method() );
            }

            for( Header h : _requestedHeaders ) {
                rb.addHeader(  h.name().toString(), h.value().toString() );
            }

            _request = rb.build();

            try {
                _future = _client.executeRequest( _request, this );
            }
            catch( IOException e ) {
                onThrowable( e );
            }
        }

        @Override
        public void onThrowable( Throwable t )
        {
            try {
                if( t instanceof Exception ) {
                    // Ignore AbortedException
                    if( !( t instanceof AbortedException ) ) {
                        setError( (Exception) t );
                        //FIXME: Complete on Aborted?
                    }
                }
                else {
                    // Re-throw will get debug logged/swallowed, so log
                    // error and exit!
                    _log.error( "Session.onThrowable: ", t );
                    System.exit( 12 );
                }
            }
            finally {
                // If we don't exit.
                complete();
            }
        }

        @Override
        public STATE onStatusReceived( HttpResponseStatus status )
        {
            _statusCode = status.getStatusCode();
            _statusText = status.getStatusText();

            URI url = status.getUrl();
            setImplHeaders( url );
            setUrl( url.toString() );

            return _state;
        }

        @Override
        public STATE onHeadersReceived( HttpResponseHeaders headers )
        {

            FluentCaseInsensitiveStringsMap hmap = headers.getHeaders();

            _responseHeaders = new ArrayList<Header>( hmap.size() + 1 );

            StringBuilder sline = new StringBuilder( 5 +
                ( ( _statusText != null ) ? _statusText.length() : 0 ) );
            sline.append( _statusCode );
            if( _statusText != null ) {
                sline.append( ' ' ).append( _statusText );
            }
            _responseHeaders.add( new Header( "Status-Line", sline ) );

            copyHeaders( hmap, _responseHeaders );

            ContentType ctype = Headers.contentType( _responseHeaders );

            if( _statusCode == 200 ) {

                if( ! acceptedContentTypes().contains( ctype ) ) {
                    _statusCode = NOT_ACCEPTED;
                    abort();
                }
                else {

                    int length = Headers.contentLength( _responseHeaders );

                    if( length > maxContentLength() ) {
                        _statusCode = TOO_LARGE_LENGTH;
                        abort();
                    }
                    else {
                        _body = new ResizableByteBuffer(
                             ( length >= 0 ) ? length : 16 * 1024 );
                    }
                }
            }

            return _state;
        }

        @Override
        public STATE onBodyPartReceived( HttpResponseBodyPart part )
        {
            if( _body != null ) {
                byte[] buffer = part.getBodyPartBytes();

                if( (_body.position() + buffer.length) > maxContentLength() ) {
                    _statusCode = TOO_LARGE;
                    abort();
                }
                else {
                    _body.put( buffer );
                }
            }
            else {
                _log.debug( "Ignoring onBodyPartReceived" );
            }

            return _state;
        }

        @Override
        public Session onCompleted()
        {
            complete();
            return this;
        }

        private void complete()
        {
            ResponseHandler handler = _handler;
            if( handler == null ) {
                throw new IllegalStateException(
                   "Handler already completed!" );
            }
            _handler = null;

            handler.sessionCompleted( this );
        }

        @SuppressWarnings("unused")
        public void waitForCompletion()
            throws InterruptedException
        {
            try {
                if( _future != null ) _future.get();
            }
            catch( ExecutionException x ) {
                //FIXME: Should be safe to ignore (already passed to handler)
            }
        }

        private void setImplHeaders( URI uri )
        {
            List<Header> implHeaders = new ArrayList<Header>( 2 );

            StringBuilder reqLine = new StringBuilder( 128 );

            reqLine.append( _request.getMethod() );
            reqLine.append( ' ' );
            String path = uri.getRawPath();
            reqLine.append( ( path != null ) ? path : '/' );
            String query = uri.getRawQuery();
            if( query != null ) {
                reqLine.append( '?' );
                reqLine.append( query );
            }

            implHeaders.add( new Header( "Request-Line", reqLine ) );

            StringBuilder host = new StringBuilder( 32 );
            host.append( uri.getHost() );
            int port = uri.getPort();
            boolean isHttps = "https".equals( uri.getScheme() );
            if( !( ( port == 80 && !isHttps ) ||
                   ( port == 443 && isHttps ) ) ) {
                host.append( ':' );
                host.append( port );
            }

            implHeaders.add( new Header( "Host", host ) );

            _implHeaders = implHeaders;
        }

        private List<Header>
        copyHeaders( FluentCaseInsensitiveStringsMap inHeaders,
                     List<Header> outHeaders )
        {
            for( Map.Entry<String, List<String>> h : inHeaders.entrySet()  ) {
                for( String v : h.getValue() ) {
                    outHeaders.add( new Header( h.getKey(), v ) );
                }
            }
            return outHeaders;
        }

        private List<Header> _requestedHeaders = new ArrayList<Header>( 8 );
        private List<Header> _implHeaders = Collections.emptyList();

        private Request _request = null;

        private ResponseHandler _handler = null;

        private int _statusCode = STATUS_UNKNOWN;
        private String _statusText = null;

        private ResizableByteBuffer _body = null;

        private ArrayList<Header> _responseHeaders;

        private volatile STATE _state = STATE.CONTINUE;

        private ListenableFuture<Session> _future;
    }

    private class AbortedException extends Exception
    {
    }

    private final AsyncHttpClient _client;

    private int _maxContentLength = 2 * 1024 * 1024;
    private ContentTypeSet _acceptedContentTypes = ContentTypeSet.ANY;

    private final Logger _log = LoggerFactory.getLogger( getClass() );
}
