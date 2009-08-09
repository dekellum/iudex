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
