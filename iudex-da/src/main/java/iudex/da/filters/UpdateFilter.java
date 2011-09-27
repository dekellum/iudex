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

package iudex.da.filters;

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

    public void update( UniMap content ) throws SQLException
    {
        ContentUpdater updater =
            new ContentUpdater( _dsource, _mapper, new UpdateTransformer() );

        updater.update( content );
    }

    @Override
    public boolean filter( UniMap content )
    {
        try {
            update( content );
        }
        catch( SQLException x ) {
            SQLException s = x;
            while( s != null ) {
                _log.error( s.getMessage() );
                s = s.getNextException();
            }
            // FIXME: Really want to treat this as fatal?
            throw new RuntimeException( x );
        }

        return true;
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

    private DataSource _dsource;
    private ContentMapper _mapper;

    private FilterContainer _updateRefFilter = new NoOpFilter();
    private FilterContainer _newRefFilter    = new NoOpFilter();
    private FilterContainer _contentFilter   = new NoOpFilter();

    protected static final Logger _log =
        LoggerFactory.getLogger( UpdateFilter.class );
}
