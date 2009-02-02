package iudex.http;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseResponseHandler implements ResponseHandler
{

    @Override
    public void handleSuccess( HTTPSession session )
    {
        try {
            handleSuccessUnsafe( session );
        }
        catch( IOException x ) {
            _log.warn( "From handleSuccess() of Url: " + session.url(), x );
        }
        finally {
            closeSession( session );
        }
    }

    @Override
    public void handleError( HTTPSession session, int code )
    {
        _log.warn( "Url: {}; Response: {} {}",
                   new Object[] { session.url(), code, session.statusText() } );
        closeSession( session );
    }


    @Override
    public void handleException( HTTPSession session, Exception x )
    {
        _log.warn( "Url: " + session.url(), x );
        closeSession( session );
    }

    protected void handleSuccessUnsafe( HTTPSession session ) 
        throws IOException
    {
    }
    
    protected void closeSession( HTTPSession session )
    {
        try {
            session.close();
            //FIXME: Catch in close() instead?        
        }
        catch( IOException x ) {
            _log.warn( "On session close: ", x );
        }
    }

    private Logger _log = LoggerFactory.getLogger( getClass() );
}
