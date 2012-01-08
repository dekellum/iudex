/*
 * Copyright (c) 2008-2012 David Kellum
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

package iudex.core.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iudex.barc.BARCDirectory;
import iudex.barc.BARCDirectory.WriteSession;
import iudex.barc.BARCFile.Record;
import iudex.core.ContentKeys;
import iudex.core.ContentSource;
import iudex.filter.Filter;
import iudex.filter.FilterException;
import iudex.http.HTTPKeys;
import iudex.http.Header;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;
import com.gravitext.util.Closeable;
import com.gravitext.util.Streams;

/**
 * Filter based writer of CONTENT to a barc directory.
 *
 * @author David Kellum
 */
public final class BARCWriter implements Filter, Closeable
{
    public BARCWriter( BARCDirectory barcDir )
    {
        _barcDir = barcDir;
    }

    public void setDoCompress( boolean doCompress )
    {
        _doCompress = doCompress;
    }

    public void setMetaHeaders( List<Key> metaHeaders )
    {
        _metaHeaders = metaHeaders;
    }

    public void setRecordType( char type )
    {
        _recordType = type;
    }

    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        try {
            write( content );
        }
        catch( InterruptedException e ) {
            _log.warn( "Write interupted: " + e );
        }
        catch( IOException e ) {
            throw new FilterException( e );
        }
        return true;
    }

    private void write( UniMap content )
        throws InterruptedException, IOException
    {
        WriteSession session = null;
        try {
            ContentSource cs = content.get( ContentKeys.SOURCE );
            if( cs != null ) {
                session = _barcDir.startWriteSession();

                replaceIfSameFile( content, session );

                Record rec = session.append();
                rec.setCompressed( _doCompress );
                rec.setType( _recordType );

                rec.writeMetaHeaders( genMetaHeaders( content ) );

                List<Header> headers = content.get( HTTPKeys.REQUEST_HEADERS );
                if( headers != null ) rec.writeRequestHeaders( headers );

                headers = content.get( HTTPKeys.RESPONSE_HEADERS );
                if( headers != null ) rec.writeResponseHeaders( headers );

                Streams.copy( cs.stream(), rec.bodyOutputStream() );
                //FIXME: Better ByteBuffer based copy?

                // Save off file number and offset.
                content.set( ContentKeys.CACHE_FILE, session.fileNumber() );
                content.set( ContentKeys.CACHE_FILE_OFFSET, rec.offset() );
            }
        }
        finally {
            if( session != null ) session.close(); //also closes record
        }
    }

    private void replaceIfSameFile( UniMap content, WriteSession session )
        throws IOException
    {
        UniMap current = content.get( ContentKeys.CURRENT );

        if( current != null ) {
            Integer cfile = current.get( ContentKeys.CACHE_FILE );
            Long coffset  = current.get( ContentKeys.CACHE_FILE_OFFSET );

            if( ( cfile != null ) && ( coffset != null ) &&
                ( cfile == session.fileNumber() ) ) {
                _log.debug( "Replacing prior record, offset {} in same file {}",
                            coffset, cfile );
                session.markReplaced( coffset );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Header> genMetaHeaders( UniMap content )
    {
        List<Header> headers = new ArrayList<Header>( _metaHeaders.size() );
        for( Key key : _metaHeaders ) {
            Object value = content.get( key );

            if( value != null ) headers.add( new Header( key, value ) );
        }
        return headers;
    }

    @Override
    public void close()
    {
        try {
            _barcDir.close();
        }
        catch( IOException e ) {
            _log.error( "On close: ", e );
        }
    }

    private final BARCDirectory _barcDir;

    private boolean _doCompress = false;
    private char _recordType = 'H'; //HTML

    private List<Key> _metaHeaders = Arrays.asList( (Key) ContentKeys.URL,
                                                    ContentKeys.LAST_VISIT,
                                                    ContentKeys.TYPE );

    private Logger _log = LoggerFactory.getLogger( getClass() );
}
