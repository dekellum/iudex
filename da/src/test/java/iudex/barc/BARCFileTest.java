package iudex.barc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import iudex.barc.BARCFile.Record;
import iudex.barc.BARCFile.RecordReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.concurrent.TestExecutor;
import com.gravitext.concurrent.TestFactoryBase;
import com.gravitext.concurrent.TestRunnable;
import com.gravitext.util.FastRandom;

public class BARCFileTest
{
    
    @Test
    public void testEmpty() throws IOException
    {
        
        Record rec = _barc.append();
        //rec.setCompressed( true );        
        assertEquals( 0L, rec.offset() );

        rec = _barc.append();
        assertEquals( BARCFile.HEADER_LENGTH, rec.offset() );
        
        rec = _barc.append();
        assertEquals( BARCFile.HEADER_LENGTH * 2, rec.offset() );
    }

    @Test
    public void testWrite() throws IOException
    {
        Record rec = _barc.append();
        rec.setCompressed( true );
            
        rec.writeMetaHeaders( 
            Arrays.asList( new Header( "Name-1", "value-1"),
                           new Header( "Name-2", "value-2") ) );
        rec.writeRequestHeaders( 
            Arrays.asList( new Header( "RQST-1", "request-value-1" ) ) );
            
        Writer writer = 
            new OutputStreamWriter( rec.bodyOutputStream(), "UTF-8" );
        writer.write( "This is the entity body.\r\n" );
        writer.write( "Line two." );
        writer.close(); //will also close record.

        rec = _barc.read( rec.offset() );
        List<Header> headers = rec.metaHeaders();
        assertEquals( 2, headers.size() );
        assertEquals( "Name-1", headers.get(0).name().toString() );
        assertEquals( "Name-2", headers.get(1).name().toString() );
        assertEquals( "value-2", headers.get(1).value().toString() );
        
        headers = rec.requestHeaders();
        assertEquals( 1, headers.size() );
        assertEquals( "request-value-1", 
                      headers.get(0).value().toString() );
        BufferedReader in = new BufferedReader( 
            new InputStreamReader( rec.bodyInputStream(), "UTF-8" ), 128 );
        assertEquals( "This is the entity body.", in.readLine() );
        assertEquals( "Line two.", in.readLine() );
        rec.close();
    }
    
    @Test
    public void testReader() throws IOException
    {
        for( int i = 0; i < 20; ++i ) {
            Record rec = _barc.append();
            if( i % 2 == 0 ) rec.setCompressed( true ); 
            try {
                rec.writeResponseHeaders( 
                    Arrays.asList( new Header( "RESP-" + i, "value-" + i ) ) );
            }
            finally {
                rec.close();
            }
        }
        
        close();
        open();
        
        RecordReader reader = _barc.reader();
        for( int i = 0; i < 20; ++i ) {
            Record rec;
            assertNotNull( rec = reader.next() );
            assertEquals( 1, rec.responseHeaders().size() );
            assertEquals( "RESP-" + i, 
                          rec.responseHeaders().get( 0 ).name().toString() );
            assertEquals( "value-" + i, 
                          rec.responseHeaders().get( 0 ).value().toString() );
            
        }
        
        assertNull( reader.next() );
    }
    
    @Test
    public void testWriteRead() throws IOException
    {
        RecordReader reader = _barc.reader();
        for( int i = 1; i <= 64; ++i ) {
            Record wrec = _barc.append();
            Record rrec = null;
            try {
                if( i > 1 ) {
                    assertNotNull( "iteration: " + i, rrec = reader.next() );
                    assertEquals( 1, rrec.responseHeaders().size() );
                    assertEquals( "RESP-" + (i-1), 
                        rrec.responseHeaders().get( 0 ).name().toString() );
                }
                
                if( i % 2 == 0 ) wrec.setCompressed( true ); 
                wrec.writeResponseHeaders( 
                    Arrays.asList( new Header( "RESP-" + i, "value-" + i ) ) );
                OutputStream out = wrec.bodyOutputStream();
                for( byte[] row : FILLER ) out.write( row );
                out.close();
            }
            finally {
                wrec.close();
                if( rrec != null ) rrec.close();
            }
            
        }
    }
    
    @Test
    public void testConcurrentReadWrite() throws IOException
    {
        long count = TestExecutor.run( new ConcurrentReadWrite(), 100, 4 );
        _log.debug( "Completed threaded run with {} iterations.", count );
    }
 
    
    class ConcurrentReadWrite extends TestFactoryBase 
    {
        @Override
        public TestRunnable createTestRunnable( final int seed )
        {
            return new TestRunnable() {
                final FastRandom _rand = new FastRandom( seed );
                public int runIteration( int run ) throws IOException
                {
                    int sum = 0;
                    for( int i = 0; i < 200; ++i ) {
                        if( _rand.nextInt( 20 ) == 0 ) {
                            sum += write();
                        }
                        else {
                            sum += read();
                        }
                    }
                    return sum;
                }
                
                private int write() throws IOException
                {
                    synchronized( _writeLock ) {
                        int i = _writeCount.get();
                        Record rec = _barc.append();
                        try {
                            if( _rand.nextInt( 3 ) == 0 ) {
                                rec.setCompressed( true );
                            }
                            rec.writeMetaHeaders( 
                                Arrays.asList( 
                                    new Header( "RESP-" + i, "value-" + i ) ) );
                            OutputStream out = rec.bodyOutputStream();
                            for( byte[] row : FILLER ) out.write( row );
                            out.close();
                            
                        }
                        finally {
                            rec.close();
                            _writeCount.getAndIncrement();
                        }
                    }
                    return -1;
                }
                
                private int read() throws IOException
                {
                    final RecordReader reader = _barc.reader();
                    Record rec;
                    int i = 0;
                    int end = Math.min( _rand.nextInt( 100 ) + 1, 
                                        _writeCount.get() ); 
                    while( ( i < end ) && 
                           ( ( rec = reader.next() ) != null ) ) {
                        assertEquals( 1, rec.metaHeaders().size() );
                        Header h = rec.metaHeaders().get( 0 );
                        assertEquals( "RESP-" + i, h.name().toString() );
                        rec.close();
                        ++i;
                    }
                    return i;
                }
            };
        }
        private Object _writeLock = new Object();
        private AtomicInteger _writeCount = new AtomicInteger(0);
        
    }
    
    
    private static byte[][] FILLER = new byte[20][];
    static {
        for( int r = 0; r < FILLER.length; ++r ) {
            FILLER[r] = new byte[ r * 4 + 1 ];
            Arrays.fill( FILLER[r], (byte) ('A' + r ) );
        }
    }
    
    public void open() throws IOException
    {
        _barc = new BARCFile( new File( "./target/test.barc" ) );
    }
    
    @Before
    public void start() throws IOException
    {
        open();
        _barc.truncate();
    }
    
    @After
    public void close() throws IOException
    {
        _barc.close();
    }
    
    private BARCFile _barc;
    private Logger _log = LoggerFactory.getLogger( getClass() );
}
