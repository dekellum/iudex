/*
 * Copyright (c) 2008-2011 David Kellum
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
package iudex.httpclient3;

import iudex.http.ContentTypeSet;
import iudex.http.HTTPClient;
import iudex.http.HTTPSession;
import iudex.http.Header;
import iudex.http.ResponseHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.RedirectException;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;

public class HTTPClient3 implements HTTPClient
{

    public HTTPClient3( HttpClient client )
    {
        _client = client;
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
    public HTTPSession createSession()
    {
        return new Session();
    }

    @Override
    public void request( HTTPSession session, ResponseHandler handler )
    {
        ((Session) session).execute( handler );
    }

    private class Session extends HTTPSession
    {
        public void addRequestHeader( Header header )
        {
            _requestedHeaders.add( header );
        }

        public List<Header> requestHeaders()
        {
            if( _httpMethod != null ) {

                org.apache.commons.httpclient.Header[] inHeaders =
                    _httpMethod.getRequestHeaders();
                List<Header> outHeaders =
                    new ArrayList<Header>( inHeaders.length + 1 );

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
            return ( _httpMethod == null ) ? null : _httpMethod.getStatusText();
        }

        public List<Header> responseHeaders()
        {
            return _responseHeaders;
        }

        public InputStream responseStream() throws IOException
        {
            return _httpMethod.getResponseBodyAsStream();
        }

        public void abort() throws IOException
        {
            if( _httpMethod != null ) {
                _httpMethod.abort();
            }
            close(); //FIXME: Good idea to also close?
        }

        public void close() throws IOException
        {
            super.close(); //FIXME: Or abstract?

            if( _httpMethod != null ) {
                _httpMethod.releaseConnection();
                _httpMethod = null;
            }
        }

        void execute( ResponseHandler handler )
        {
            try {
                if( method() == Method.GET ) {
                    _httpMethod = new GetMethod( url() );
                }
                else if( method() == Method.HEAD ) {
                    _httpMethod = new HeadMethod( url() );
                }

                for( Header h : _requestedHeaders ) {
                    _httpMethod.addRequestHeader( h.name().toString(),
                                                  h.value().toString() );
                }

                try {
                    _responseCode = _client.executeMethod( _httpMethod );
                }
                catch( RedirectException e ) {
                    // Just get the 3xx status code and continue as normal.
                    _responseCode = _httpMethod.getStatusCode();
                }

                copyResponseHeaders();

                // Record possibly redirected URL.
                setUrl( _httpMethod.getURI().toString() );

                if( ( _responseCode >= 200 ) && ( _responseCode < 300 ) ) {
                    handler.handleSuccess( this );
                }
                else {
                    handler.handleError( this, _responseCode );
                }
            }
            catch( IOException e ) {
                handler.handleException( this, e );
            }
        }

        private CharSequence reconstructRequestLine()
        {
            StringBuilder reqLine = new StringBuilder( 128 );
            reqLine.append( _httpMethod.getName() );
            reqLine.append( ' ' );
            reqLine.append( _httpMethod.getPath() );
            if( _httpMethod.getQueryString() != null ) {
                reqLine.append( '?' );
                reqLine.append( _httpMethod.getQueryString() );
            }
            return reqLine;
        }

        private void copyResponseHeaders()
        {
            org.apache.commons.httpclient.Header[] inHeaders =
                _httpMethod.getResponseHeaders();

            List<Header> outHeaders =
                new ArrayList<Header>( inHeaders.length + 1 );

            StatusLine statusLine = _httpMethod.getStatusLine();
            if( statusLine != null ) {
                outHeaders.add( new Header( "Status-Line", statusLine ) );
            }

            copyHeaders( inHeaders, outHeaders );
            _responseHeaders = outHeaders;
        }

        private List<Header>
        copyHeaders( org.apache.commons.httpclient.Header[] inHeaders,
                     List<Header> outHeaders )
        {
            for( org.apache.commons.httpclient.Header h : inHeaders  ) {
                outHeaders.add( new Header( h.getName(), h.getValue() ) );
            }
            return outHeaders;
        }

        private List<Header> _requestedHeaders = new ArrayList<Header>( 8 );
        private List<Header> _responseHeaders = Collections.emptyList();
        private int _responseCode = 0;

        private HttpMethod _httpMethod = null;
    }

    private HttpClient _client;
    private int _maxContentLength = 2 * 1024 * 1024;
    private ContentTypeSet _acceptedContentTypes = ContentTypeSet.ANY;
}
