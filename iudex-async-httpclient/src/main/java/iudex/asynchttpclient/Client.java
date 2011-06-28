/*
 * Copyright (c) 2011 David Kellum
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.util.Closeable;
import com.gravitext.util.ResizableByteBuffer;
import com.gravitext.util.Streams;
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
        return new Session();
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

                FluentCaseInsensitiveStringsMap inHeaders = _request.getHeaders();

                List<Header> outHeaders =
                    new ArrayList<Header>( inHeaders.size() + 1 );

                outHeaders.add( new Header( "Request-Line",
                                            reconstructRequestLine() ) );

                copyHeaders( inHeaders, outHeaders );

                return outHeaders;
            }
            return _requestedHeaders;
        }

        public int responseCode()
        {
            return _responseCode;
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

        public InputStream responseStream() throws IOException
        {
            return Streams.inputStream( _body.flipAsByteBuffer() );
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

        public void close() throws IOException
        {
            super.close(); //FIXME: NoOp for now?
        }

        void execute( ResponseHandler handler )
        {
            _handler = handler;

            BoundRequestBuilder rb = null;

            if( method() == Method.GET ) {
                rb = _client.prepareGet( url() );
            }
            else if( method() == Method.HEAD ) {
                rb = _client.prepareHead( url() );
            }
            else {
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
            if( t instanceof Exception ) {
                // Ignore AbortedException
                if( !( t instanceof AbortedException ) ) {
                    _handler.handleException( this, (Exception) t );
                }
            }
            else {
                // Re-throw will get debug logged/swallowed, so log error and
                // exit!
                _log.error( "Session.onThrowable: ", t );
                System.exit( 12 );
            }
        }

        @Override
        public STATE onStatusReceived( HttpResponseStatus status )
        {
            _responseCode = status.getStatusCode();
            _statusText = status.getStatusText();

            setUrl( status.getUrl().toString() );

            return _state;
        }

        @Override
        public STATE onHeadersReceived( HttpResponseHeaders headers )
        {

            FluentCaseInsensitiveStringsMap hmap = headers.getHeaders();

            _responseHeaders = new ArrayList<Header>( hmap.size() + 1 );

            _responseHeaders.add( new Header( "Status-Line",
                String.valueOf( _responseCode ) + " " + _statusText ) );
            //FIXME: Incomplete: create full status

            copyHeaders( hmap, _responseHeaders );

            ContentType ctype = Headers.contentType( _responseHeaders );

            if( ! _acceptedContentTypes.contains( ctype ) ) {
                _responseCode = -20; //FIXME: Constants in iudex.http?
                abort();
            }

            if( _state == STATE.CONTINUE ) {

                int length = Headers.contentLength( _responseHeaders );

                if( length > _maxContentLength ) {
                    _responseCode = -10;
                    abort();
                }
                else {
                    _body = new ResizableByteBuffer(
                        ( length >= 0 ) ? length : 16 * 1024 );
                }
            }

            return _state;
        }

        @Override
        public STATE onBodyPartReceived( HttpResponseBodyPart part )
        {
            byte[] buffer = part.getBodyPartBytes();

            if( ( _body.position() + buffer.length ) > _maxContentLength ) {
                _responseCode = -11;
                abort();
            }
            else {
                _body.put( buffer );
            }

            return _state;
        }

        @Override
        public Session onCompleted()
        {
            if( ( _responseCode >= 200 ) && ( _responseCode < 300 ) ) {
                _handler.handleSuccess( this );
            }
            else {
                _handler.handleError( this, _responseCode );
            }

            return this;
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

        private CharSequence reconstructRequestLine()
        {
            StringBuilder reqLine = new StringBuilder( 128 );

            reqLine.append( _request.getMethod() );
            reqLine.append( ' ' );
            reqLine.append( _request.getUrl() );

            //FIXME: Not correct
            //reqLine.append( '?' );
            //_request.getQueryParams();

            return reqLine;
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

        private Request _request = null;

        private ResponseHandler _handler = null;

        private int _responseCode = 0;
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
