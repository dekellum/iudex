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

import java.util.Date;

import com.gravitext.htmap.Key;

public class ContentKeys
{
    private static <T> Key<T> create( String name, Class<T> valueType )
    {
        return Content.KEY_SPACE.create( name, valueType );
    }

    public static final Key<VisitURL> URL = 
        create( "url", VisitURL.class ); 

    public static final Key<CharSequence> TITLE = 
        create( "title", CharSequence.class );
    
    public static final Key<ContentSource> CONTENT =
        create( "content", ContentSource.class );
    
    public static final Key<Date> PUBLISHED_DATE = 
        create( "published_date", Date.class );
    
    /**
     * Highest priority wins.
     */
    public static final Key<Double> PRIORITY = 
        create( "priority", Double.class );

    public static enum Type {
        FEED,
        PAGE,
        ROBOTS_TXT,
        SITE_MAP
    }

    public static final Key<Type> TYPE = 
        create( "type", Type.class );
    
}
