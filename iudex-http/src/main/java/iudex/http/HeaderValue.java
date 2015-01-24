/*
 * Copyright (c) 2008-2015 David Kellum
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
import java.util.List;

/**
 * Generic parsed Header value representation.
 *
 * Supports parsing the following generalized RFC 2616 HTTP Header value syntax:
 *
 * <pre>
 * elements      = [ element ] *( "," [ element ] )
 * element       = parameter *( ";" [ parameter ] )
 * parameter     = [ [ name ] "=" ] value
 * name          = token
 * value         = 1*( token | quoted-string )
 * token         = 1*<any char except "=", ",", ";", <"> and WS>
 * quoted-string = <"> *( text | quoted-char ) <">
 * text          = any char except <">
 * quoted-char   = "\" char
 * </pre>
 *
 * The parser is liberal and should not throw an exception for non-null input.
 * Note headers such as Date which use HTTP-date do not follow the above
 * pattern and should be parsed by others means.
 */
public class HeaderValue
{
    /**
     * Represents a parameter value with optional name.
     * Names,values are trimmed of whitespace.
     * Quoted string values are de-escaped and quotes dropped.
     * Empty name,values are normalized to null.
     */
    public final static class Parameter
    {
        public String name = null;
        public CharSequence value = null;
    }

    public HeaderValue()
    {
    }

    public static HeaderValue parseFirst( final CharSequence in )
    {
        final HeaderValue v = new HeaderValue();
        parseNext( in, 0, v );
        return v;
    }

    public static List<HeaderValue> parseAll( final CharSequence in )
    {
        final List<HeaderValue> values = new ArrayList<HeaderValue>( 8 );
        int p = 0;
        final int end = in.length();
        while( p < end ) {
            final HeaderValue v = new HeaderValue();
            p = parseNext( in, p, v );
            if( ! v.parameters().isEmpty() ) values.add( v );
        }
        return values;
    }

    /**
     * Return first parameter in potential series. Often the value of this
     * parameter is the entire usable header value.
     *
     */
    public final Parameter first()
    {
        return ( _parameters.isEmpty() ) ? null : _parameters.get( 0 );
    }

    /**
     * Return value of first parameter, which is often the entire desired
     * header value.
     */
    public final CharSequence firstValue()
    {
        Parameter first = first();
        return ( first == null ) ? null : first.value;
    }

    public final List<Parameter> parameters()
    {
        return _parameters;
    }

    public final Parameter findAnyCase( String name )
    {
        for( Parameter p : _parameters ) {
            if( name.equalsIgnoreCase( p.name ) ) {
                return p;
            }
        }
        return null;
    }

    protected void setParameters( List<Parameter> params )
    {
        _parameters = params;
    }

    static int parseNext( final CharSequence in,
                          int p,
                          final HeaderValue hvalue )
    {

        final List<Parameter> params = new ArrayList<Parameter>( 4 );

        Parameter param = new Parameter();

        StringBuilder buff = new StringBuilder( 32 );

        boolean quoted = false;
        int b          = p;
        final int  end = in.length();
        while( p < end ) {
            char c = in.charAt(p);
            if( quoted ) {
                if     ( c == '\\' ) { if( b < p ) buff.append( in, b, p );
                                       b = ++p;
                                       if( p < end ) ++p; }
                else if( c == '"'  ) { buff.append( in, b, ++p );
                                       quoted = false;
                                       b = p; }
                else ++p;
            }
            else {
                if     ( c == ','  ) { if( b < p ) buff.append( in, b, p );
                                       b = ++p;
                                       break; }
                else if( c == ';'  ) { if( b < p ) buff.append( in, b, p );
                                       param.value = trim( buff );
                                       buff = new StringBuilder( 32 );
                                       if( param.value != null ) {
                                             params.add( param );
                                             param = new Parameter();
                                       }
                                       b = ++p; }
                else if( c == '='  ) { if( b < p ) buff.append( in, b, p );
                                       param.name = trimToString( buff );
                                       buff = new StringBuilder( 32 );
                                       b = ++p; }
                else if( c == '"'  ) { buff.append( in, b, ++p );
                                       quoted = true;
                                       b = p; }
                else ++p;
            }
        }
        if( b < p ) buff.append( in, b, p );
        if( buff.length() > 0 )   param.value = trim( buff );
        if( param.value != null ) params.add( param );

        hvalue.setParameters( params );

        return p;
    }

    /**
     *  Trim away single-quotes from in and return subsequence or null if
     *  empty or in is null. Optional extra lenience applied externally.
     */
    public static CharSequence trimSQuote( final CharSequence in )
    {
        if( in != null ) {
            int b = 0;
            int e = in.length();
            if( ( e > 2 ) &&
                ( in.charAt( 0 ) == '\'' ) && ( in.charAt( e-1 ) == '\'' ) ) {
                ++b; --e;
            }
            if( ( b == 0 ) && ( e == in.length() ) ) return in;
            if( b < e ) return in.subSequence( b, e );
        }
        return null;
    }

    /**
     * Trim away lead/trailing whitespace, then if quoted value (both sides),
     * trim quotes as well. Return null if empty or in is null.
     */
    private static CharSequence trim( final CharSequence in )
    {
        if( in != null ) {
            int b = 0;
            int e = in.length();

            while( ( b < e ) && isWS( in.charAt(   b ) ) ) ++b;
            while( ( e > b ) && isWS( in.charAt( e-1 ) ) ) --e;

            if( ( b < ( e-1 ) ) &&
                ( in.charAt( b ) == '"' ) && ( in.charAt( e-1 ) == '"' ) ) {
                ++b; --e;
            }
            if( ( b == 0 ) && ( e > 0 ) && ( e == in.length() ) ) return in;
            if( b < e ) return in.subSequence( b, e );
        }
        return null;
    }

    private static String trimToString( CharSequence buff )
    {
        buff = trim( buff );
        return ( buff == null ) ? null : buff.toString();
    }

    private static boolean isWS( char c )
    {
        return ( ( c == ' '  ) || ( c == '\t' ) ||
                 ( c == '\r' ) || ( c == '\n' ) );
    }

    private List<Parameter> _parameters = null;
}
