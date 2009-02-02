package iudex.http;

import iudex.core.Header;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

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
    
    public abstract Iterable<Header> requestHeaders();
    public abstract int responseCode();
    public abstract String statusText();
    public abstract Iterable<Header> responseHeaders();
    public abstract InputStream responseStream() throws IOException;
    
    public void close() throws IOException
    {
    }

    private String _url;
    //private List<Header> _requestHeaders;
    private Method _method = Method.GET;
}
