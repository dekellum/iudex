package iudex.http;

/**
 * HTTP Client abstraction. This interface is used to hide HTTP client 
 * implementation details and enable pluggability. In particular synchronous 
 * (executes in this thread) and asynchronous (call handler in the future) modes 
 * are abstracted.
 */
public interface HTTPClient
{
    /**
     * Create a new generic session, suitable for adding request details and 
     * calling request().  
     */
    public HTTPSession createSession();
    
    /**
     * Perform request as defined by session and relay results to handler. This 
     * call may return immediately or block until initial request processing is
     * complete. In any case, the handler should call session.close() when the 
     * session is no longer needed.  
     */
    public void request( HTTPSession session, ResponseHandler handler );
}
