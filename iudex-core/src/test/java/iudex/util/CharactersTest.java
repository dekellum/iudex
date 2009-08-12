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

package iudex.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CharactersTest
{
    @Test
    public void testClean()
    {
        assertColl( "",  "" );
        assertColl( "",  "\u0000" ); //NUL
        assertColl( "",  "\uFFFE" ); //BAD BOM
        assertColl( "",  "\t \r\n" );
        assertColl( "",  "\u00A0\u2007\u202f" );

        assertColl( "x",  "x"   );
        assertColl( "x", " x  " );
        assertColl( "x", " x"   );
        assertColl( "x",  "x "  );

        assertColl( "aa b c", "aa b c" );
        assertColl( "aa b c", "aa \t b c" );
        assertColl( "aa b c", "\t aa \t b c" );
    }

    private void assertColl( String to, String from )
    {
        assertEquals( to, Characters.cleanCtrlWS( from ).toString() );
    }
}
