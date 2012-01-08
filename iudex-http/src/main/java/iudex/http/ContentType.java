/*
 * Copyright (c) 2008-2012 David Kellum
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

/**
 * Extends HeaderValue with custom parser and accessors for the
 * Content-Type header.
 */
public class ContentType extends HeaderValue
{
    /**
     * Returns (mime) type/sub-type trimmed and normalized
     * (from first header element).
     */
    public final String type()
    {
        return _type;
    }

    /**
     * Return "charset" parameter value if specified (trimmed of whitespace
     * or quotes.)
     */
    public final String charset()
    {
        return _charset;
    }

    public static ContentType parse( final CharSequence in )
    {
        ContentType ctype = new ContentType();

        parseNext( in, 0, ctype );

        CharSequence type = ctype.firstValue();
        if( type != null ) {
            ctype._type = type.toString().toLowerCase();
        }

        Parameter charset = ctype.findAnyCase( "charset" );
        if( charset != null ) {
            CharSequence cvalue = trimSQuote( charset.value ); //Leniency++
            if( cvalue != null ) ctype._charset = cvalue.toString();
        }

        return ctype;
    }

    private String _type = null;
    private String _charset = null;
}
