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
package iudex.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public abstract class HTTPSession implements Closeable
{
    public static enum Method {
        GET,
        HEAD
    }

    /**
     * Psuedo-HTTP status code: as yet unknown.
     */
    public static final int STATUS_UNKNOWN   =   0;

    /**
     * Psuedo-HTTP status code: ERROR delivered as exception
     */
    public static final int ERROR            =  -1;

    /**
     * Psuedo-HTTP status code: Critical (i.e. Throwable non-Exception)
     * delivered as exception or re-thrown.
     */
    public static final int ERROR_CRITICAL   =  -2;

    /**
     * Psuedo-HTTP status code: Response body too large by Content-Length
     * header.
     */
    public static final int TOO_LARGE_LENGTH = -10;

    /**
     * Psuedo-HTTP status code: Response body found too large upon reading.
     */
    public static final int TOO_LARGE        = -11;

    /**
     * Psuedo-HTTP status code:
     */
    public static final int NOT_ACCEPTED     = -20;

    public String url()
    {
        return _url;
    }
    public void setUrl( String url )
    {
        _url = url;
    }
    public Method method()
    {
        return _method;
    }
    public void setMethod( Method method )
    {
        _method = method;
    }

    public void addRequestHeaders( List<Header> headers )
    {
        for( Header h : headers ) {
            addRequestHeader( h );
        }
    }

    public abstract void addRequestHeader( Header header );

    public abstract List<Header> requestHeaders();

    /**
     * Return HTTP 1.1 status code or Psuedo-code as per above constants:
     * zero or negative.
     */
    public abstract int statusCode();

    public abstract String statusText();

    public abstract List<Header> responseHeaders();

    public abstract InputStream responseStream() throws IOException;

    public void close() throws IOException
    {
    }

    public void abort() throws IOException
    {
    }

    private String _url;
    private Method _method = Method.GET;
}
