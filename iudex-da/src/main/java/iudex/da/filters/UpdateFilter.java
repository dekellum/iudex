/*
 * Copyright (c) 2008-2015 David Kellum
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
import java.util.ArrayList;

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
    public static final NoOpFilter DEFAULT_MERGE = new NoOpFilter();

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

    /**
     * Set additional chained filters to run the original content once all
     * references are processed but before a final content update is made.
     * The default NoOpFilter does a simple merge from current (db) to
     * updated (memory). Setting to null disables content updates entirely.
     */
    public void setContentFilter( FilterContainer contentFilter )
    {
        _contentFilter = contentFilter;
    }

    /**
     * Set additional chained filters to run on the REFERER of content before
     * the final update is made. The default NoOpFilter does a simple merge
     * from current (db) to updated (memory).
     * Setting to null disables referer updates entirely.
     */
    public void setRefererFilter( FilterContainer refererFilter )
    {
        _refererFilter = refererFilter;
    }

    public void setUpdateRefFilter( FilterContainer updateRefFilter )
    {
        _updateRefFilter = updateRefFilter;
    }

    public void setNewRefFilter( FilterContainer newRefFilter )
    {
        _newRefFilter = newRefFilter;
    }

    public void setIsolationLevel( int isolationLevel )
    {
        _isolationLevel = isolationLevel;
    }

    public int isolationLevel()
    {
        return _isolationLevel;
    }

    /**
     * Set max number of retries, not including the initial try.
     * Default: 3
     */
    public void setMaxRetries( int count )
    {
        _maxRetries = count;
    }

    @Override
    public boolean filter( UniMap content )
    {
        try {
            update( content );
            return true;
        }
        catch( SQLException x ) {
            // Already logged by ContentUpdater (ContentWriter's logger)
            return false;
        }
    }

    public void update( UniMap content ) throws SQLException
    {
        new Updater().update( content );
    }

    private final class Updater extends ContentUpdater
    {
        Updater() {
            super( _dsource, _mapper, new UpdateTransformer() );
            setIsolationLevel( _isolationLevel );
            setMaxRetries( _maxRetries );
            setUpdateReferences( _updateRefFilter != null &&
                                 _newRefFilter != null );
            setUpdateReferer( _refererFilter != null );
            setUpdateContent( _contentFilter != null );
        }

        @Override
        protected boolean handleError( int tries, SQLException x )
            throws InterruptedException
        {
            boolean retry = super.handleError( tries, x );
            if( retry ) ( (UpdateTransformer) transformer() ).reset();
            return retry;
        }
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
        public UniMap transformReferer( UniMap updated, UniMap current )
        {
            UniMap out = merge( updated, current );

            out.set( CURRENT, current );
            out.set( NEW_REFERENCES,     _newReferences );
            out.set( UPDATED_REFERENCES, _updatedReferences );

            if( ! _refererFilter.filter( out ) ) out = null;

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

        void reset() {
            _newReferences = 0;
            _updatedReferences = 0;
        }

        private int _newReferences = 0;
        private int _updatedReferences = 0;
    }

    @Override
    public List<FilterContainer> children()
    {
        List<FilterContainer> list = new ArrayList<FilterContainer>();

        if( _updateRefFilter != null ) list.add( _updateRefFilter );
        if( _newRefFilter    != null ) list.add( _newRefFilter  );
        if( _contentFilter   != null ) list.add( _contentFilter );
        if( _refererFilter   != null ) list.add( _refererFilter );

        return list;
    }

    @Override
    public void close()
    {
        if( _updateRefFilter != null ) _updateRefFilter.close();
        if( _newRefFilter    != null ) _newRefFilter.close();
        if( _contentFilter   != null ) _contentFilter.close();
        if( _refererFilter   != null ) _refererFilter.close();
    }

    private static final List<Key> REQUIRED_KEYS =
        Arrays.asList( new Key[] { UHASH } );

    private final DataSource _dsource;
    private final ContentMapper _mapper;
    private int _isolationLevel = Connection.TRANSACTION_REPEATABLE_READ;
    private int _maxRetries = 3;

    private FilterContainer _updateRefFilter = DEFAULT_MERGE;
    private FilterContainer _newRefFilter    = DEFAULT_MERGE;
    private FilterContainer _contentFilter   = DEFAULT_MERGE;
    private FilterContainer _refererFilter   = DEFAULT_MERGE;

    protected static final Logger _log =
        LoggerFactory.getLogger( UpdateFilter.class );
}
