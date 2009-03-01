package iudex.http.barc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import iudex.barc.BARCFile;
import iudex.barc.BARCFile.Record;
import iudex.core.Header;
import iudex.http.BaseResponseHandler;
import iudex.http.HTTPSession;

public class BARCResponseHandler extends BaseResponseHandler
{

    public BARCResponseHandler( BARCFile barcFile )
    {
        _barcFile = barcFile;
    }
    
    public void setDoCompress( boolean doCompress )
    {
        _doCompress = doCompress;
    }
    
    public boolean copyComplete() 
    {
        return _complete;
    }
    
    @Override
    protected void handleSuccessUnsafe( HTTPSession session )
        throws IOException
    {
        Record rec = _barcFile.append();
        try {
            rec.setCompressed( _doCompress );
            rec.writeMetaHeaders( Arrays.asList( 
                new Header( "url", session.url() ) ) );
            rec.writeRequestHeaders( session.requestHeaders() );
            rec.writeResponseHeaders( session.responseHeaders() );
            _complete = copy( session.responseStream(), 
                              rec.bodyOutputStream() );
        }
        finally {
            rec.close();
        }
    }
    
    private boolean copy( InputStream in, OutputStream out ) 
        throws IOException
    {
        int len = 0;
        int total = 0;
        final byte[] buff = new byte[ BUFFER_SIZE ];
        while( true ) {
            len = in.read( buff );
            if( len <= 0 ) break;
            out.write( buff, 0, len );
            if( ( total += len ) > _maxBodyLength ) return false; 
        }
        return true;
    }
    
    private int _maxBodyLength = 10 * 1024 * 1024; //10M default 
    private static final int BUFFER_SIZE = 2048;
    private final BARCFile _barcFile;
    private boolean _doCompress = false;
    private boolean _complete = false;
}
