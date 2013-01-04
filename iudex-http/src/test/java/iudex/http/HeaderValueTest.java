/*
 * Copyright (c) 2008-2013 David Kellum
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

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.gravitext.util.FastRandom;

public class HeaderValueTest
{
    @Test
    public void testSingleToken()
    {
        assertEquals( "token", value( "token", 0 ) );
        assertEquals( "token", value( " token", 0 ) );
        assertEquals( "token", value( " token  ", 0 ) );
        assertEquals( "token", value( "token; other", 0 ) );
        assertEquals( "other", value( "token; other", 1 ) );

        assertEquals( "two  tokens", value( "\"two  tokens\"", 0 ) );
        assertEquals( "two\" tokens ", value( "\"two\\\" tokens \"", 0 ) );

        assertEquals( "token", value( "token;", 0 ) );
        assertEquals( "token", value( "token,", 0 ) );

    }

    @Test
    public void testNameValue()
    {
        assertEquals( "name",   name( "name=value", 0 ) );
        assertEquals( "value", value( "name=value", 0 ) );
        assertEquals( "value", value( "name=\"value\"", 0 ) );

        assertEquals( "va;ue", value( "name=\"va;ue\"", 0 ) );
        assertEquals( "va=ue", value( "name=\"va=ue\"", 0 ) );
        assertEquals( "va,ue", value( "name=\"va,ue\"", 0 ) );

        assertEquals( "name",   name( "x ;name=\"va;ue\"", 1 ) );
        assertEquals( "va;ue", value( "x ;name=\"va;ue\"", 1 ) );

        assertEquals( "va;ue", value( "x=y;name=\"va;ue\"", 1 ) );
        assertEquals( "va;ue", value( "x=\"\\\";,y\";name=\"va;ue\"", 1 ) );
        assertEquals( "\";,y", value( "x=\"\\\";,y\";name=\"va;ue\"", 0 ) );
    }

    @Test
    public void testCompound()
    {
        assertEquals( "W/\"two  tokens\"", value( "W/\"two  tokens\"", 0 ) );
        assertEquals( "one \"two  tokens\"",
                      value( "one \"two  tokens\"", 0 ) );
    }
    @Test
    public void testNone()
    {
        assertTrue( HeaderValue.parseAll( "" ).isEmpty() );
        assertTrue( HeaderValue.parseAll( " , " ).isEmpty() );
        assertTrue( HeaderValue.parseAll( "," ).isEmpty() );
    }
    @Test
    public void testFirstEmpty()
    {
        assertEquals( "token", value( ",token", 0 ) );
        assertNull( HeaderValue.parseFirst( ",t" ).first() );
        assertNull( HeaderValue.parseFirst( ",t" ).firstValue() );
    }
    @Test
    public void testNonTerminatedString()
    {
        assertEquals( "\"badness", value( "\"badness", 0 ) );
        assertEquals( "badness\"", value( "badness\"", 0 ) );
        assertEquals( "\"badness", value( "\"badness\\", 0 ) );

    }
    @Test
    public void testRandom()
    {
        final char[] chars = " \t\n\rnpx=,;\\\"\'".toCharArray();
        assertEquals( 13, chars.length );

        FastRandom rand = new FastRandom(666);
        for( int runs = 0; runs < 7919; ++runs ) {
            int len = rand.nextInt( 67 );
            StringBuilder b = new StringBuilder( len );
            while( len > 0 ) {
                b.append( chars[ rand.nextInt( chars.length ) ] );
                --len;
            }

            List<HeaderValue> hvals = HeaderValue.parseAll( b );
            for( HeaderValue hv : hvals ) {
                assertFalse( hv.parameters().isEmpty() );
                for( HeaderValue.Parameter p : hv.parameters() ) {
                    assertTrue( ( p.value.length() > 0 ) );
                    assertTrue( ( p.name  == null ) || ! p.name.isEmpty() );
                }
            }
        }
    }

    private String value( String in, int i )
    {
        HeaderValue v = HeaderValue.parseAll( in ).get( 0 );

        return v.parameters().get( i ).value.toString();
    }
    private String name( String in, int i )
    {
        HeaderValue v = HeaderValue.parseAll( in ).get( 0 );

        return v.parameters().get( i ).name;
    }

}
