package iudex.core;

/**
 * Exception thrown by a {@link ContentFilter} signaling content reject by
 * failure.
 */
public class FilterException extends Exception
{
    public FilterException( String message )
    {
        super( message );
    }

    public FilterException( Throwable cause )
    {
        super( cause );
    }

    public FilterException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
