/*
 * Copyright (C) 2008-2009 David Kellum
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

package iudex.http.filters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.ResizableByteBuffer;

import iudex.core.ContentKeys;
import iudex.core.ContentSource;
import iudex.filter.AsyncFilterContainer;
import iudex.filter.Filter;
import iudex.filter.FilterContainer;
import iudex.http.BaseResponseHandler;
import iudex.http.HTTPClient;
import iudex.http.HTTPKeys;
import iudex.http.HTTPSession;
import iudex.http.ContentType;
import iudex.http.Headers;

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

    public Set<String> acceptedContentTypes()
    {
        return _acceptedContentTypes;
    }

    public boolean filter( UniMap content )
    {
        HTTPSession session = _client.createSession();
        session.setMethod( HTTPSession.Method.GET );
        session.setUrl( content.get( ContentKeys.URL ).toString() );

        //FIXME: Set ETAG?
        //FIXME: Set Since (from LAST_VISIT)
        //FIXME: Set REFERER (URL)?

        _client.request( session, new Handler( content ) );

        return true;
    }

    @Override
    public List<Filter> children()
    {
        return Arrays.asList( new Filter[] { _receiver } );
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
            throws IOException
        {
            setHTTPValues( session );

            if( !testContentType( session ) ) {
                session.abort();
                handleError( session, -20 );
                return;
            }

            int len = Headers.contentLength(
                _content.get( HTTPKeys.RESPONSE_HEADERS ) );
            if( len != 0 ) {
                if( len > _maxContentLength ){
                    session.abort();
                    handleError( session, -10 );
                    return;
                }

                ResizableByteBuffer buffer
                    = new ResizableByteBuffer( (len > 0) ? len : 16 * 1024 );

                buffer.putFromStream( session.responseStream(),
                                      _maxContentLength + 1, 8 * 1024 );

                if( buffer.position() > _maxContentLength ) {
                    session.abort();
                    handleError( session, -10 );
                    return;
                }

                ContentSource cs =
                    new ContentSource( buffer.flipAsByteBuffer() );

                _content.set( ContentKeys.CONTENT, cs );
            }
            // FIXME: Catch IO Exception, handle and pass to _receiver?
            _receiver.filter( _content );
        }

        private boolean testContentType( HTTPSession session )
        {
            if( _acceptedContentTypes != null ) {
                ContentType ctype =
                    Headers.contentType( session.responseHeaders() );
                if( ( ctype != null ) && ctype.type() != null ) {
                    return _acceptedContentTypes.contains( ctype.type() );
                }
            }
            return true;
        }

        @Override
        public void handleError( HTTPSession session, int code )
        {
            super.handleError( session, code );
            setHTTPValues( session );
            _receiver.filter( _content );
        }

        private void setHTTPValues( HTTPSession session )
        {
            _content.set( HTTPKeys.REQUEST_HEADERS, session.requestHeaders() );
            _content.set( ContentKeys.STATUS, session.responseCode() );
            _content.set( HTTPKeys.RESPONSE_HEADERS, session.responseHeaders() );
        }

        private final UniMap _content;
    }

    private final int _maxContentLength = 1024 * 1024 - 1;

    private final HTTPClient _client;
    private FilterContainer _receiver;
    private Set<String> _acceptedContentTypes = null;
}
