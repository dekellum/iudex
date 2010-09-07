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

import static org.junit.Assert.*;

import org.junit.Test;

public class TokenCounterTest
{
    @Test
    public void test()
    {
        assertSimHash( 0x0000000000000000L, "" );
        assertSimHash( 0x0000000000000000L, " " );

        assertSimHash( 0x6be0c71a2296a2ecL, "one" );

        assertSimHash( 0xd93a3da70c07e87aL, "Spaces not significant." );
        assertSimHash( 0xd93a3da70c07e87aL, " Spaces  not significant.  " );

        assertSimHash( 0x636958e44a00c540L,
                       "A much longer string to be simhashed, " +
                       "hopefully enough to see good mixing, i.e. the" +
                       "full key is used. " );

        assertSimHash( 0x7310f1a44f10e544L,
                       "An even bigger string to be simhashed, " +
                       "clearly enough to see good mixing, i.e. the" +
                       "full key is used." );
    }

    private void assertSimHash( long ehash, String chars )
    {
        TokenCounter counter = new TokenCounter();
        counter.add( chars );

        long shash = counter.simhash();
        assertEquals( hex( ehash ) + " != " + hex( shash ),
                      ehash, shash );
    }

    private String hex( long shash )
    {
        return Long.toHexString( shash );
    }
}
