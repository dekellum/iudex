/*
 * Copyright (c) 2008-2010 David Kellum
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility methods for List<Header>
 */
public final class Headers
{
    public static Header getFirst( final List<Header> headers,
                                   final String name )
    {
        for( Header h : headers ) {
            if( name.equalsIgnoreCase( h.name().toString() ) ) {
                return h;
            }
        }
        return null;
    }

    /**
     * Returns parsed integer value of any specified Content-Length header
     * or -1, if not specified or invalid.
     */
    public static int contentLength( final List<Header> headers )
    {
        int length = -1;
        Header h = getFirst( headers, "Content-Length" );
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

    public static ContentType contentType( final List<Header> headers  )
    {
        Header h = getFirst( headers, "Content-Type" );

        CharSequence tvalue = asCharSequence( h.value() );
        if( tvalue != null ) return ContentType.parse( tvalue );
        return null;
    }

    public static CharSequence asCharSequence( Object in )
    {
        if( in instanceof CharSequence ) return (CharSequence) in;
        if( in != null )                 return in.toString();
        return null;
    }

    /**
     * Create a header from name and Date value in HTTP-date (1.1) format
     * RFC 1123 (update/subset of RFC 822),.
     */
    public static Header createDateHeader( Object name, Date value )
    {
        // Create new each time for thread safety.
        SimpleDateFormat df = new SimpleDateFormat( HTTP_DATE_FORM, Locale.US );
        df.setTimeZone( TZ_GMT );
        return new Header( name, df.format( value ) );
    }

    private static final String HTTP_DATE_FORM =
        "EEE, dd MMM yyyy HH:mm:ss 'GMT'";

    private static final TimeZone TZ_GMT = TimeZone.getTimeZone( "GMT" );
}
