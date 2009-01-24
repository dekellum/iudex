package iudex.core;

public class ParseException extends Exception
{
    public ParseException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ParseException( String message )
    {
        super( message );
    }

    public ParseException( Throwable cause )
    {
        super( cause );
    }

    private static final long serialVersionUID = 1L;
}
