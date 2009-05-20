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
    
    public abstract List<Header> requestHeaders();
    public abstract int responseCode();
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
