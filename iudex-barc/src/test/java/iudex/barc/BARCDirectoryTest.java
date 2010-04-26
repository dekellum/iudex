/*
 * Copyright (c) 2008-2010 David Kellum
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
import iudex.barc.BARCDirectory.WriteSession;
import iudex.barc.BARCFile.Record;
import iudex.http.Header;

import java.io.File;
import java.io.IOException;
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

public class BARCDirectoryTest
{
    @Test
    public void testWriteRead() throws IOException, InterruptedException
    {
        WriteSession session = _barcs.startWriteSession();
        int fnum = session.fileNumber();

        Record rec = session.append();
        long offset = rec.offset();
        rec.writeMetaHeaders( Arrays.asList(
            new Header( "META-1", "meta-value-1" ) ) );
        rec.close();

        session.close();

        close();
        open();

        rec = _barcs.read( fnum, offset );
        List<Header> headers = rec.metaHeaders();
        assertEquals( 1, headers.size() );
        assertEquals( "META-1", headers.get( 0 ).name().toString() );
    }

    @Test
    public void testConcurrentWrite() throws IOException
    {
        _barcs.setTargetBARCLength( 200 ); //~3 records/file

        long count = TestExecutor.run( new ConcurrentWriter(), 200, 3 );
        _log.debug( "Completed threaded run with {} iterations.", count );
    }

    @Before
    public void start() throws IOException
    {
        File[] bfiles = BARC_DIR.listFiles();
        if( bfiles != null ) {
            for( File bfile : bfiles ) bfile.delete();
        }
        BARC_DIR.delete();

        open();
    }

    @After
    public void close() throws IOException
    {
        if( _barcs != null ) {
            _barcs.close();
            _barcs = null;
        }
    }
    private void open() throws IOException
    {
        _barcs = new BARCDirectory( BARC_DIR );
    }

    private final class ConcurrentWriter extends TestFactoryBase
    {
        @Override
        public TestRunnable createTestRunnable( final int seed )
        {
            return new TestRunnable() {
                public int runIteration( int run )
                    throws IOException, InterruptedException
                {
                    WriteSession session = _barcs.startWriteSession();
                    Record rec = session.append();
                    rec.writeMetaHeaders( Arrays.asList(
                        new Header( "File-Number", session.fileNumber() ),
                        new Header( "Offset", rec.offset() ) ) );

                    rec.close();
                    session.close();
                    return 1;
                }
            };
        }
    }

    private BARCDirectory _barcs;
    private static final File BARC_DIR = new File( "./target/test_barc_dir" );
    private Logger _log = LoggerFactory.getLogger( getClass() );
}
