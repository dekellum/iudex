/*
 * Copyright (c) 2010 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package iudex.simhash.gen;

import static com.gravitext.util.Charsets.UTF_8;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class ByteTokenizerTest
{
    @Test
    public void test()
    {
        assertTokens( "" );
        assertTokens( " " );
        assertTokens( "  " );

        assertTokens( "a",  "a" );
        assertTokens( " a", "a" );
        assertTokens( "a ", "a" );

        assertTokens( "ab",  "ab" );
        assertTokens( " ab", "ab" );
        assertTokens( "ab ", "ab" );

        assertTokens( "a b", "a", "b" );

        assertTokens( "ab cde fg",   "ab", "cde", "fg" );
        assertTokens( " ab cde fg",  "ab", "cde", "fg" );
        assertTokens( "ab  cde fg",  "ab", "cde", "fg" );
        assertTokens( "ab cde fg ",  "ab", "cde", "fg" );
        assertTokens( "ab cde fg  ", "ab", "cde", "fg" );
    }

    private void assertTokens( String input, String... tokens )
    {
        ByteTokenizer tokenizer = new ByteTokenizer( bb( input ) );

        for( String t : tokens ) {
            assertTrue( tokenizer.hasNext() );
            String next = str( tokenizer.next() );
            assertEquals( t, next );
        }
        assertFalse( tokenizer.hasNext() );
    }

    private String str( ByteBuffer next )
    {
        return UTF_8.decode( next ).toString();
    }

    private ByteBuffer bb( String input )
    {
        return UTF_8.encode( input );
    }

}
