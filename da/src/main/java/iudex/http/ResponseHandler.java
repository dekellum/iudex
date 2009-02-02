package iudex.http;

public interface ResponseHandler
{
    public void handleSuccess( HTTPSession session );

    public void handleError( HTTPSession session, int code );
    
    public void handleException( HTTPSession session, Exception x );
}
