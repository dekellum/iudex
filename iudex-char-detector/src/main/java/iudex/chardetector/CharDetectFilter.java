package iudex.chardetector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Charsets;

import iudex.core.ContentSource;
import iudex.filter.Filter;
import iudex.filter.FilterException;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import static iudex.core.ContentKeys.SOURCE;

public class CharDetectFilter implements Filter
{
    public int maxDetectLength()
    {
        return _maxDetectLength;
    }

    public void setMaxDetectLength( int maxDetectLength )
    {
        _maxDetectLength = maxDetectLength;
    }

    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        ContentSource source = content.get( SOURCE );

        if( source != null ) {
            CharsetDetector detector = new CharsetDetector();
            detector.setDeclaredEncoding( source.defaultEncoding().name() );

            boolean textSet = false;

            if( source.source() instanceof ByteBuffer ) {
                ByteBuffer b = (ByteBuffer) source.source();
                if( b.hasArray() && ! b.isReadOnly() &&
                    b.arrayOffset() == 0 && b.position() == 0 ) {
                    byte[] ba = b.array();
                    if( ( b.limit() == ba.length ) &&
                        ( b.limit() <= _maxDetectLength ) ) {
                        detector.setText( ba );
                        textSet = true;
                    }
                }
                if( ! textSet ) {
                    b = b.slice();
                    byte[] ba = new byte[ Math.min( b.remaining(),
                                                    _maxDetectLength ) ];
                    b.get( ba );
                    detector.setText( ba );
                    textSet = true;
                }
            }

            if( ! textSet ) {
                try {
                    detector.setText( source.stream() );
                }
                catch( IOException e ) {
                    throw new RuntimeException( e );
                }
                textSet = true;
            }

            CharsetMatch match = detector.detect();

            if( match != null ) {
                Charset cs = Charsets.lookup( match.getName() );
                if( cs != null ) {
                    source.setDefaultEncoding( cs );
                    source.setEncodingConfidence( match.getConfidence() /
                                                  100.0F );
                }
            }
        }
        return true;
    }

    private int _maxDetectLength = 64 * 1024;
}
