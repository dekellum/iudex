package iudex.barc;

/**
 * A name/value pair suitable for HTTP and other header representation. All  
 * name and value instances must support toString() as serialized from. 
 * In addition 
 * 
 */
public final class Header
{

    public Header( Object name, Object value )
    {
        if( name == null ) throw new NullPointerException( "name" );
        if( value == null ) throw new NullPointerException( "value" );
        
        _name = name;
        _value = value;
    }

    public Object name()
    {
        return _name;
    }
    public Object value()
    {
        return _value;
    }

    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder( 128 );
        append( _name, b );
        b.append( ": " );
        append( _value, b );
        return b.toString();
    }
    
    private void append( Object it, StringBuilder b )
    {
        if( it instanceof CharSequence ) b.append( (CharSequence ) it );
        else b.append( it.toString() );
    }


    private final Object _name;
    private final Object _value;
}
