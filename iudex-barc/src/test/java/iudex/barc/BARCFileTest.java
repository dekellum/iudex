/*
 * Copyright (c) 2008-2011 David Kellum
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
package iudex.barc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import iudex.barc.BARCFile.Record;
import iudex.barc.BARCFile.RecordReader;
import iudex.http.Header;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.concurrent.TestExecutor;
import com.gravitext.concurrent.TestFactoryBase;
import com.gravitext.concurrent.TestRunnable;
import com.gravitext.util.FastRandom;
import static com.gravitext.util.Charsets.UTF_8;

public class BARCFileTest
{

    @Test
    public void testEmpty() throws IOException
    {

        Record rec = _barc.append();
        rec.setType( 'T' );
        rec.setCompressed( true );
        assertEquals( 0L, rec.offset() );

        rec = _barc.append();
        rec.setType( 'T' );
        assertEquals( BARCFile.HEADER_LENGTH, rec.offset() );

        rec = _barc.append();
        rec.setType( 'T' );
        rec.setCompressed( true );
        assertEquals( BARCFile.HEADER_LENGTH * 2, rec.offset() );
        rec.close();
        rec = _barc.read( rec.offset() );
        assertEquals( -1, rec.bodyInputStream().read( new byte[ 1024 ] ) );
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
            new OutputStreamWriter( rec.bodyOutputStream(), UTF_8 );
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
            new InputStreamReader( rec.bodyInputStream(), UTF_8 ), 128 );
        assertEquals( "This is the entity body.", in.readLine() );
        assertEquals( "Line two.", in.readLine() );
        rec.close();
    }

    @Test
    public void testCopy() throws IOException
    {
        Record rec = _barc.append();

        rec.writeMetaHeaders(
            Arrays.asList( new Header( "Name-1", "value-1"),
                           new Header( "Name-2", "value-2") ) );
        rec.writeRequestHeaders(
            Arrays.asList( new Header( "RQST-1", "request-value-1" ) ) );

        Writer writer =
            new OutputStreamWriter( rec.bodyOutputStream(), UTF_8 );
        writer.write( "This is the entity body.\r\n" );
        writer.write( "Line two just in the middle in this case.\r\n" );
        writer.write( "Line three for some extra length filler business." );
        writer.close(); //will also close record.

        rec = _barc.read( rec.offset() );
        BARCFile barc2 = new BARCFile( new File( "./target/copy.barc" ) );
        try {
            barc2.truncate();
            Record crec = barc2.append();
            crec.setCompressed( true );
            crec.copyFrom( rec );

        }
        finally {
            rec.close();
            barc2.close();
        }

        barc2 = new BARCFile( new File( "./target/copy.barc" ) );
        try {
            rec = barc2.read( 0 );

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
                new InputStreamReader( rec.bodyInputStream(), UTF_8 ), 128 );
            assertEquals( "This is the entity body.", in.readLine() );
            rec.close();
        }
        finally {
            barc2.close();
        }
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
    public void testMarkReplaced() throws IOException
    {
        Record wrec = _barc.append();
        wrec.setType( 'T' );
        wrec.writeResponseHeaders(
            Arrays.asList( new Header( "RESP", "value" ) ) );
        OutputStream out = wrec.bodyOutputStream();
        for( byte[] row : FILLER ) out.write( row );
        out.close();
        wrec.close();
        _barc.markReplaced( 0 );
        wrec = _barc.append();
        wrec.close();
    }

    @Test
    public void testConcurrentReadWrite() throws IOException
    {
        long count = TestExecutor.run( new ConcurrentReadWrite(), 2000, 8 );
        _log.debug( "Completed threaded run with {} iterations.", count );
    }

    private final class ConcurrentReadWrite extends TestFactoryBase
    {
        @Override
        public TestRunnable createTestRunnable( final int seed )
        {
            return new TestRunnable() {
                final FastRandom _rand = new FastRandom( seed );
                public int runIteration( int run ) throws IOException
                {
                    return ( _rand.nextInt(7) == 0 ) ? write() : read();
                }

                private int write() throws IOException
                {
                    synchronized( _writeLock ) {
                        int i = _writeCount;
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
                            ++_writeCount;
                        }
                    }
                    return 1;
                }

                private int read() throws IOException
                {
                    final RecordReader reader = _barc.reader();
                    Record rec;
                    int i = 0;
                    int r = 0;
                    while( ( rec = reader.next() ) != null ) {
                        if( _rand.nextInt( 4 ) == 0 ) {
                            assertEquals( 1, rec.metaHeaders().size() );
                            Header h = rec.metaHeaders().get( 0 );
                            assertEquals( "RESP-" + i, h.name().toString() );
                            ++r;
                        }
                        rec.close();
                        ++i;
                    }
                    return r;
                }
            };
        }
        private Object _writeLock = new Object();
        private int _writeCount = 0;

    }

    private static byte[][] FILLER = new byte[20][];
    static {
        for( int r = 0; r < FILLER.length; ++r ) {
            FILLER[r] = new byte[ r * 4 + 1 ];
            Arrays.fill( FILLER[r], (byte) ( 'A' + r ) );
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
