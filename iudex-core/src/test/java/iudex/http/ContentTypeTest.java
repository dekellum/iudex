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

import static org.junit.Assert.*;

import org.junit.Test;

public class ContentTypeTest
{
    @Test
    public void testSimple()
    {
        assertEquals( "text/html", type( "text/html" ) );
        assertEquals( "text/html", type( " text/Html " ) );
    }

    @Test
    public void testNoCharSet()
    {
        assertNull( charset( ",text/html" ) );
        assertNull( charset( " text/html  " ) );
        assertNull( charset( " text/html;" ));

        assertEquals( "text/html", type(" text/html; name=value") );
        assertNull( charset( " text/html; name=value" ) );
        assertNull( charset( " text/html; charset=  " ) );
    }

    @Test
    public void testCharset()
    {
        assertEquals( "text/html", type( "Text/html; charset=UTF-8" ) );

        assertEquals( "UTF-8", charset( "Text/html;foo; charset=UTF-8\r" ) );
        assertEquals( "UTF-8", charset( "Text/html; charset=\"UTF\\-8\"" ) );
        assertEquals( "UTF-8", charset( "Text/html;charset= 'UTF-8'" ));

        assertEquals( "UTF-8", charset( "Text/html ; charset=UTF-8;qs=0.8" ) );
        assertEquals( "UTF-8", charset("Text/html;qs=0.8;charset= \"UTF-8\"") );
    }

    private String type( CharSequence in )
    {
        ContentType ctype = ContentType.parse( in );
        return ctype.type();
    }

    private String charset( CharSequence in )
    {
        ContentType ctype = ContentType.parse( in );
        return ctype.charset();
    }

}
