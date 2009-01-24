package iudex.core;

/**
 * A name/value pair suitable for HTTP and other header representation. All  
 * name and value instances must support toString() as serialized from.
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
        b.append( _name.toString() );
        b.append( ": " );
        b.append(  _value.toString() );
        return b.toString();
    }
    
    private final Object _name;
    private final Object _value;
}
