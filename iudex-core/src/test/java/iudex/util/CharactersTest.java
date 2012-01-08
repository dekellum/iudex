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

package iudex.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CharactersTest
{
    @Test
    public void testCleanTrim()
    {
        assertCleanTrim( "", "" );
        assertCleanTrim( "", "\u0000" ); //NUL
        assertCleanTrim( "", "\uFFFE" ); //BAD BOM
        assertCleanTrim( "", "\t \r\n" );
        assertCleanTrim( "", "\u00A0\u2007\u202f" );

        assertCleanTrim( "x",  "x"   );
        assertCleanTrim( "x", " x  " );
        assertCleanTrim( "x", " x"   );
        assertCleanTrim( "x",  "x "  );

        assertCleanTrim( "aa b c", "aa b c "      );
        assertCleanTrim( "aa b c", "aa \t b c"    );
        assertCleanTrim( "aa b c", "\t aa \t b c" );
    }

    @Test
    public void testClean()
    {
        assertClean( "",  ""  );
        assertClean( ".", " " );

        assertClean( "x",   "x"    );
        assertClean( ".x.", " x  " );
        assertClean( ".x",  " x"   );
        assertClean( "x.",  "x "   );

        assertClean( "aa.b.c.", "aa b c "      );
        assertClean( "aa.b.c",  "aa \t b c"    );
        assertClean( ".aa.b.c", "\t aa \t b c" );
    }

    @Test
    public void testCount()
    {
        assertCount( 0, ""  );
        assertCount( 0, " " );

        assertCount( 1, "x"    );
        assertCount( 1, " x"   );
        assertCount( 1, " x  " );

        assertCount( 1, "xyz" );

        assertCount( 2, "xyz b" );
        assertCount( 2, "x-z b" );
        assertCount( 2, "--z b" );
        assertCount( 2, "x-- b" );
        assertCount( 2, "-- x-- b -" );
        assertCount( 2, "- x-- b --" );
    }

    private void assertCleanTrim( String to, String from )
    {
        assertEquals( to, Characters.cleanCtrlWS( from ).toString() );
    }

    private void assertClean( String to, String from )
    {
        CharSequence clean = Characters.replaceCtrlWS( from, ".", false );
        assertEquals( to, clean.toString() );
    }

    private void assertCount( int count, String from )
    {
        assertEquals( count, Characters.wordCount( from ) );
    }

}
