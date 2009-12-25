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

package iudex.da;

import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

public class DataAccessKeys
{
    protected static <T> Key<T> create( String name, Class<T> valueType )
    {
        return UniMap.KEY_SPACE.create( name, valueType );
    }

    protected static <T> Key<List<T>> createListKey( String name )
    {
        return UniMap.KEY_SPACE.createListKey( name );
    }

    /**
     * During update operations, the CURRENT DB content.
     */
    public static final Key<UniMap> CURRENT =
        create( "current", UniMap.class );

}
