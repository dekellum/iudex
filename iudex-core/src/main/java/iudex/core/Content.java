package iudex.core;

import com.gravitext.htmap.ArrayHTMap;
import com.gravitext.htmap.HTAccess;
import com.gravitext.htmap.Key;
import com.gravitext.htmap.KeySpace;

/**
 * A generic Content key/value container.
 * @author David Kellum
 */
public final class Content 
    implements HTAccess
{
    public static final KeySpace KEY_SPACE = new KeySpace();

    /**
     *  {@inheritDoc}
     */
    public <T> T get( Key<T> key )
    {
        return _fields.get( key );
    }

    /**
     * {@inheritDoc}  In this implementation, if a null value is given then 
     * the key is removed.
     */
    public <T, V extends T> T set( Key<T> key, V value )
    {
        if( value != null ) {
            return _fields.set( key, value );
        }
        else {
            return remove( key );
        }
    }
    /**
     * {@inheritDoc}
     */
    public <T> T remove( Key<T> key )
    {
        return _fields.remove( key );
    }
    
    @Override
    public String toString() 
    {
        return _fields.toString();
    }
    
    private ArrayHTMap _fields = new ArrayHTMap( KEY_SPACE );
}
