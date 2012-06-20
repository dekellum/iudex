/*
 * Copyright (c) 2008-2012 David Kellum
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

package iudex.da.filters;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import iudex.da.BaseTransformer;
import iudex.da.ContentMapper;
import iudex.da.ContentUpdater;
import iudex.filter.FilterContainer;
import iudex.filter.NoOpFilter;

import static iudex.core.ContentKeys.*;
import static iudex.da.ContentMapper.*;

/**
 * Filter based support for updating references (from feeds, etc.) and the
 * original content as part of a single batch transaction.
 */
public class UpdateFilter implements FilterContainer
{
    public UpdateFilter( DataSource source, ContentMapper mapper )
    {
        for( Key req : REQUIRED_KEYS ) {
            if( ! mapper.fields().contains( req ) ) {
                throw new IllegalArgumentException(
                   "UpdateFilter needs mapper with " + req + " included." );
            }
        }

        _dsource = source;
        _mapper = mapper;
    }

    public void setUpdateRefFilter( FilterContainer updateRefFilter )
    {
        _updateRefFilter = updateRefFilter;
    }

    public void setNewRefFilter( FilterContainer newRefFilter )
    {
        _newRefFilter = newRefFilter;
    }

    /**
     * Set additional chained filters to run the original content once all
     * references are processed but before a final content update is made.
     */
    public void setContentFilter( FilterContainer contentFilter )
    {
        _contentFilter = contentFilter;
    }

    public void setIsolationLevel( int isolationLevel )
    {
        _isolationLevel = isolationLevel;
    }

    public int isolationLevel()
    {
        return _isolationLevel;
    }

    public void setRetryCount( int count )
    {
        _retryCount = count;
    }

    public void update( UniMap content ) throws SQLException
    {
        ContentUpdater updater =
            new ContentUpdater( _dsource, _mapper, new UpdateTransformer() );
        updater.setIsolationLevel( _isolationLevel );

        updater.update( content );
    }

    @Override
    public boolean filter( UniMap content )
    {
        int tries = 0;
        retry: while( true ) {
            try {
                ++tries;
                update( content );
                if( tries > 1 ) {
                    _log.info( "Update succeeded only after {} attempts",
                               tries );
                }
                return true;
            }
            catch( SQLException x ) {
                if( tries <= _retryCount ) {
                    SQLException s = x;
                    while( s != null ) {
                        String state = s.getSQLState();
                        // PostgreSQL Unique Key (i.e. uhash) violation or
                        // any Transaction Rollback should be retried
                        if( ( state != null ) &&
                            ( state.equals( "23505" ) ||
                              state.startsWith( "40" ) ) ) {
                            _log.debug( "Retry {} after: ({}) {}",
                                        new Object[] {
                                            tries, state, s.getMessage() } );
                            continue retry;
                        }
                        s = s.getNextException();
                    }
                }

                SQLException s = x;
                while( s != null ) {
                    _log.error( "Last try {}: ({}) {}",
                                new Object[] {
                                    tries, s.getSQLState(), s.getMessage() } );
                    s = s.getNextException();
                }
                break retry; //Unhandled error for retry purposes
            }
        }

        return false;
    }

    private final class UpdateTransformer extends BaseTransformer
    {

        @Override
        public UniMap transformContent( UniMap updated, UniMap current )
        {
            UniMap out = merge( updated, current );

            out.set( CURRENT, current );
            out.set( NEW_REFERENCES,     _newReferences );
            out.set( UPDATED_REFERENCES, _updatedReferences );

            if( ! _contentFilter.filter( out ) ) out = null;

            return out;
        }

        @Override
        public UniMap transformReference( UniMap updated, UniMap current )
        {
            UniMap out = merge( updated, current );

            out.set( CURRENT, current );

            if( current == null ) {
                if(    _newRefFilter.filter( out ) ) ++_newReferences;
                else out = null;
            }
            else {
                if( _updateRefFilter.filter( out ) ) ++_updatedReferences;
                else out = null;
            }
            return out;
        }

        private int _newReferences = 0;
        private int _updatedReferences = 0;
    }

    @Override
    public List<FilterContainer> children()
    {
        return Arrays.asList( _updateRefFilter, _newRefFilter, _contentFilter );
    }

    @Override
    public void close()
    {
        _updateRefFilter.close();
        _newRefFilter.close();
        _contentFilter.close();
    }

    private static final List<Key> REQUIRED_KEYS =
        Arrays.asList( new Key[] { UHASH } );

    private final DataSource _dsource;
    private final ContentMapper _mapper;
    private int _isolationLevel = Connection.TRANSACTION_REPEATABLE_READ;
    private int _retryCount = 3;

    private FilterContainer _updateRefFilter = new NoOpFilter();
    private FilterContainer _newRefFilter    = new NoOpFilter();
    private FilterContainer _contentFilter   = new NoOpFilter();

    protected static final Logger _log =
        LoggerFactory.getLogger( UpdateFilter.class );
}
