package iudex.core;

import java.io.InputStream;
import java.nio.charset.Charset;

public class ContentSource
{
    
    public InputStream stream()
    {
        return ( ( _source instanceof InputStream ) ? 
                 (InputStream) _source : null );
    }

    public CharSequence characters()
    {
        return ( ( _source instanceof CharSequence ) ? 
                 (CharSequence) _source : null ); 
    }
    
    public Charset defaultEncoding()
    {
        return _defaultEncoding;
    }

    public Object source()
    {
        return _source;
    }

    private Object _source;
    private Charset _defaultEncoding = null;

}
