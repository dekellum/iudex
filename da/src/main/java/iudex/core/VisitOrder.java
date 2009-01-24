package iudex.core;


/**
 * VisitURL plus: type (intended use), priority
 *  
 */
public class VisitOrder implements Comparable<VisitOrder>
{
    public static enum Type {
        FEED,
        PAGE,
        ROBOTS_TXT,
        SITE_MAP
    }

    public VisitOrder( VisitURL url,
                       Type type,
                       Double priority )
    {
        _url = url;
        _type = type;
        _priority = priority;
    }

    public VisitURL url()
    {
        return _url;
    }

    public Type type()
    {
        return _type;
    }

    public Double priority()
    {
        return _priority;
    }

    /**
     * Order by descending priority.
     */
    public int compareTo( VisitOrder o )
    {
        return Double.compare( o.priority(), this.priority() );
    }

    private final VisitURL _url;
    private final Type _type;
    private final Double _priority;
}
