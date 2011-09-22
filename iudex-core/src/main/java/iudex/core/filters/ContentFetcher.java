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

package iudex.core.filters;

import static iudex.core.ContentKeys.*;
import static iudex.http.HTTPKeys.*;

import iudex.core.ContentSource;
import iudex.core.VisitCounter;
import iudex.filter.AsyncFilterContainer;
import iudex.filter.FilterContainer;
import iudex.http.ContentType;
import iudex.http.ContentTypeSet;
import iudex.http.HTTPClient;
import iudex.http.HTTPSession;
import iudex.http.Header;
import iudex.http.Headers;
import iudex.http.ResponseHandler;
import iudex.util.Charsets;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.UniMap;

public class ContentFetcher implements AsyncFilterContainer
{
    public ContentFetcher( HTTPClient client,
                           VisitCounter counter,
                           FilterContainer receiver )
    {
        _client = client;
        _visitCounter = counter;
        _receiver = receiver;
    }

    /**
     * Set an executor to run receiver.filter() with, instead of
     * the thread calling the Handler callback.
     */
    public void setExecutor( Executor executor )
    {
        _executor = executor;
    }

    /**
     * Set accepted mime types.
     */
    public void setAcceptedContentTypes( ContentTypeSet types )
    {
        _acceptedContentTypes = types;
    }

    public ContentTypeSet acceptedContentTypes()
    {
        return _acceptedContentTypes;
    }

    /**
     * Set maximum length of content body to download in bytes.
     */
    public void setMaxContentLength( int maxContentLength )
    {
        _maxContentLength = maxContentLength;
    }

    public int maxContentLength()
    {
        return _maxContentLength;
    }

    public void setRequestHeaders( List<Header> headers )
    {
        _fixedRequestHeaders = headers;
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

        session.setMaxContentLength( _maxContentLength );
        session.setAcceptedContentTypes( _acceptedContentTypes );

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

    private final class Handler implements ResponseHandler
    {
        public Handler( UniMap content )
        {
            _content = content;
        }

        @Override
        public void sessionCompleted( HTTPSession session )
        {
            try {
                recordSession( session );

                if( _executor != null ) {
                    _executor.execute( new FilterTask( _content ) );
                }
                else {
                    _receiver.filter( _content );
                }
            }
            finally {
                session.close();

                // Release the (old URL) order
                _visitCounter.release( _content, null );
            }
        }

        private void recordSession( HTTPSession session )
        {
            _content.set( STATUS, session.statusCode() );
            _content.set( REQUEST_HEADERS, session.requestHeaders() );
            _content.set( RESPONSE_HEADERS, session.responseHeaders() );

            Exception error = session.error();
            if( error != null ) {
                _content.set( REASON, "i.c.f.ContentFetcher: " + error );
                _log.warn( "Url: {} :: {}", session.url(), error.toString() );
                _log.debug( "Stack Trace: ", error );
            }
            else if( ( session.statusCode() <  200 ) ||
                     ( session.statusCode() >  307 ) ) {
                _log.warn( "Url: {}; Response: {} {}",
                           new Object[] { session.url(),
                                          session.statusCode(),
                                          session.statusText() } );
            }

            List<Header> headers = _content.get( RESPONSE_HEADERS );
            if( headers == null ) {
                headers = Collections.emptyList();
            }

            Header etag = Headers.getFirst( headers, "ETag" );
            if( etag != null ) {
                _content.set( ETAG, Headers.asCharSequence( etag.value() ) );
            }

            ByteBuffer body = session.responseBody();

            if( ( body != null ) && ( body.remaining() > 0 ) ) {
                ContentSource cs = new ContentSource( body );

                // Set default encoding
                cs.setDefaultEncoding( _defaultEncoding );

                // Set better default if charset in Content-Type
                ContentType ctype = Headers.contentType( headers );
                if( ctype != null ) {
                    String eName = ctype.charset();
                    if( eName != null ) {
                        Charset enc = Charsets.lookup( eName );
                        if( enc != null ) cs.setDefaultEncoding( enc, 0.10F );
                    }
                }

                _content.set( SOURCE, cs );
            }
        }

        private final UniMap _content;
    }

    private final class FilterTask implements Runnable
    {
        FilterTask( UniMap content )
        {
            _content = content;
        }

        @Override
        public void run()
        {
            _receiver.filter( _content );
        }

        private final UniMap _content;
    }

    private final HTTPClient _client;
    private final VisitCounter _visitCounter;
    private final FilterContainer _receiver;

    private Executor _executor = null;

    private List<Header> _fixedRequestHeaders = Collections.emptyList();
    private ContentTypeSet _acceptedContentTypes = ContentTypeSet.ANY;
    private int _maxContentLength = 1024 * 1024 - 1;
    private Charset _defaultEncoding = Charsets.defaultCharset();

    private final Logger _log = LoggerFactory.getLogger( ContentFetcher.class );
}
