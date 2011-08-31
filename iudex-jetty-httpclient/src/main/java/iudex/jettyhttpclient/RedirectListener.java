/*
 * Copyright (c) 2011 David Kellum
 * Copyright (c) 2008-2009 Mort Bay Consulting Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package iudex.jettyhttpclient;

import java.io.IOException;

import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.HttpEventListener;
import org.eclipse.jetty.client.HttpEventListenerWrapper;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpSchemes;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Buffer;

/**
 * Attempt to fix RedirectListener
 */
public class RedirectListener extends HttpEventListenerWrapper
{
    private final HttpExchange _exchange;
    private HttpDestination _destination;
    private String _location;
    private int _attempts;
    private boolean _requestComplete;
    private boolean _responseComplete;
    private boolean _redirected;

    public RedirectListener(HttpDestination destination, HttpExchange ex)
    {
        // Start of sending events through to the wrapped listener
        // Next decision point is the onResponseStatus
        super(ex.getEventListener(),true);

        _destination = destination;
        _exchange = ex;
    }

    @Override
    public void onResponseStatus( Buffer version, int status, Buffer reason )
        throws IOException
    {
        _redirected = ((status == HttpStatus.MOVED_PERMANENTLY_301 ||
                        status == HttpStatus.MOVED_TEMPORARILY_302) &&
                       _attempts < _destination.getHttpClient().maxRedirects());

        if (_redirected)
        {
            setDelegatingRequests(false);
            setDelegatingResponses(false);
        }

        super.onResponseStatus(version,status,reason);
    }

    @Override
    public void onResponseHeader( Buffer name, Buffer value )
        throws IOException
    {
        if (_redirected)
        {
            int header = HttpHeaders.CACHE.getOrdinal(name);
            switch (header)
            {
                case HttpHeaders.LOCATION_ORDINAL:
                    _location = value.toString();
                    break;
            }
        }
        super.onResponseHeader(name,value);
    }

    @Override
    public void onRequestComplete() throws IOException
    {
        _requestComplete = true;

        if (checkExchangeComplete())
        {
            super.onRequestComplete();
        }
    }

    @Override
    public void onResponseComplete() throws IOException
    {
        _responseComplete = true;

        if (checkExchangeComplete())
        {
            super.onResponseComplete();
        }
    }

    public boolean checkExchangeComplete()
        throws IOException
    {
        if (_redirected && _requestComplete && _responseComplete)
        {
            if (_location != null)
            {
                if (_location.indexOf("://")>0)
                    _exchange.setURL(_location);
                else
                    _exchange.setURI(_location);

                // destination may have changed
                boolean isHttps = HttpSchemes.HTTPS.equals(String.valueOf(_exchange.getScheme()));
                HttpDestination destination=_destination.getHttpClient().getDestination(_exchange.getAddress(),isHttps);

                if (_destination==destination)
                    _destination.resend(_exchange);
                else
                {
                    // unwrap to find ultimate listener.
                    HttpEventListener listener=this;
                    while(listener instanceof HttpEventListenerWrapper)
                        listener=((HttpEventListenerWrapper)listener).getEventListener();
                    //reset the listener
                    _exchange.getEventListener().onRetry();
                    _exchange.reset();
                    _exchange.setEventListener(listener);

                    // DEK: Fixup the Host header
                    Address adr = _exchange.getAddress();
                    int port = adr.getPort();
                    StringBuilder hh = new StringBuilder( 64 );
                    hh.append( adr.getHost() );
                    if( !( ( port == 80 && !isHttps ) ||
                           ( port == 443 && isHttps ) ) ) {
                        hh.append( ':' );
                        hh.append( port );
                    }
                    _exchange.setRequestHeader( HttpHeaders.HOST, hh.toString() );

                    destination.send(_exchange);
                }

                return false;
            }
            else
            {
                setDelegationResult(false);
            }
        }

        return true;
    }

    public void onRetry()
    {
        _redirected=false;
        _attempts++;

        setDelegatingRequests(true);
        setDelegatingResponses(true);

        _requestComplete=false;
        _responseComplete=false;

        super.onRetry();
    }

    /**
     * DEK: Delegate failed connection
     */
    @Override
    public void onConnectionFailed( Throwable ex )
    {
        setDelegatingRequests(true);
        setDelegatingResponses(true);

        super.onConnectionFailed( ex );
    }

    /**
     * DEK: Delegate onException
     */
    @Override
    public void onException( Throwable ex )
    {
        setDelegatingRequests(true);
        setDelegatingResponses(true);

        super.onException( ex );
    }
}
