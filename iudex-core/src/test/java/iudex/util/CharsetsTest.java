package iudex.util;

import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class CharsetsTest {

    /**
     * Test all mappings from the whatwg guidelines that are supported by the JVM.
     * 
     * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/parsing.html#character-encodings-0
     */
    @Test
    public void testExpand() {
        Map<Charset, Charset> expectedMapping = new HashMap<Charset, Charset>();
        expectedMapping.put( Charsets.EUC_KR, Charsets.WINDOWS_949 );
        expectedMapping.put( Charsets.GB2312, Charsets.GBK );
        expectedMapping.put( Charsets.ISO_8859_1, Charsets.WINDOWS_1252 );
        expectedMapping.put( Charsets.ISO_8859_9, Charsets.WINDOWS_1254 );
        expectedMapping.put( Charsets.ISO_8859_11, Charsets.WINDOWS_874 );
        expectedMapping.put( Charsets.KS_C_5601_1987, Charsets.WINDOWS_949 );
        expectedMapping.put( Charsets.SHIFT_JIS, Charsets.WINDOWS_31J );
        expectedMapping.put( Charsets.TIS_620, Charsets.WINDOWS_874 );
        expectedMapping.put( Charsets.ASCII, Charsets.WINDOWS_1252 );
        
        for ( Map.Entry<Charset, Charset> entry : expectedMapping.entrySet() ) {
            assertEquals( entry.getValue(), Charsets.expand(entry.getKey()) );
        }
    }

    @Test
    public void testDefaultCharset() {
        assertEquals(Charsets.WINDOWS_1252, Charsets.defaultCharset());
    }

}
