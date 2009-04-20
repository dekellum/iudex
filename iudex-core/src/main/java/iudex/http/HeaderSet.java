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

package iudex.http;

import java.util.ArrayList;
import java.util.Collection;

public class HeaderSet 
    extends ArrayList<Header>
{
    public HeaderSet()
    {
        super();
    }

    public HeaderSet( Collection<? extends Header> c )
    {
        super( c );
    }

    public HeaderSet( int initialCapacity )
    {
        super( initialCapacity );
    }

    public Header getFirst( String name )
    {
        for( Header h : this ) {
            if( name.equalsIgnoreCase( h.name().toString() ) ) {
                return h;
            }
        }
        return null;
    }
    
    /**
     * Returns parsed integer value of any specified Content-Length header
     * or -1, if not specified or invalid
     */
    public int contentLength()
    {
        int length = -1;
        Header h = getFirst( "Content-Length" );
        if( h != null ) {
           try {
               length = Integer.parseInt( h.value().toString().trim() );
               if( length < -1 ) length = -1;
           }
           catch( NumberFormatException x ) {
               //ignore
           }
        }
        return length;
    }

    public ContentType contentType()
    {
        Header h = getFirst( "Content-Type" );

        CharSequence tvalue = asCharSequence( h.value() );
        if( tvalue != null ) return ContentType.parse( tvalue );
        return null;
    }
    
    public CharSequence asCharSequence( Object in )
    {
        if( in instanceof CharSequence ) return (CharSequence) in;
        if( in != null )                 return in.toString();
        return null;
    }
    
    private static final long serialVersionUID = 1L;

}
