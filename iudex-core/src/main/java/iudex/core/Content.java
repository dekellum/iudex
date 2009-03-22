/*
 * Copyright (C) 2008-2009 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * {@inheritDoc}  In this implementation, if a null value is given then 
     * the key is removed.
     */
    public Object put( Key<?> key, Object value )
    {
        if( value != null ) {
            return _fields.put( key, value );
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
