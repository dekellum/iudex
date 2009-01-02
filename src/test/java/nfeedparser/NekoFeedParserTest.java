package nfeedparser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.junit.Test;
import static org.junit.Assert.*;

public class NekoFeedParserTest
{
    @Test
    public void test() throws UnsupportedEncodingException, ParseException
    {
        byte[] ml =
            ( "<rss version=\"2.0\">\n" +
              " <channel>\n" +
              "  <item>\n" +
              "   <description><p>Example description</p></description>\n" +
              "  </item>\n" +
              " </channel>\n" +
              "</rss>" ).getBytes( "UTF-8" );

        NekoFeedParser parser = new NekoFeedParser();
        Charset defCharset = Charset.forName( "UTF-8" );
        InputStream inStream = new ByteArrayInputStream( ml );
        
        parser.parse( inStream, defCharset, false );
        
    }
}
