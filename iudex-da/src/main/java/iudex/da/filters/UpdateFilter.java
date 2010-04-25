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

package iudex.da.filters;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
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
import static iudex.da.DataAccessKeys.*;

// FIXME: Generalize: Not just for feeds.

/**
 * Filter based support for writing references (from feeds, etc.)
 */
public class UpdateFilter implements FilterContainer
{
    public UpdateFilter( DataSource source, List<Key> fields )
    {
        _dsource = source;

        LinkedHashSet<Key> mergedKeys = new LinkedHashSet<Key>( BASE_KEYS );
        mergedKeys.addAll( fields );
        _mapper = new ContentMapper( new ArrayList<Key>( mergedKeys ) );
    }

    public void setUpdateRefFilter( FilterContainer updateRefFilter )
    {
        _updateRefFilter = updateRefFilter;
    }

    public void setNewRefFilter( FilterContainer newRefFilter )
    {
        _newRefFilter = newRefFilter;
    }

    public void update( UniMap content ) throws SQLException
    {
        ContentUpdater updater =
            new ContentUpdater( _dsource, _mapper,
                                new FeedTransformer( content ) );

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
            // FIXME: As in fatal?
            throw new RuntimeException( x );
        }

        return true;
    }

    private final class FeedTransformer extends BaseTransformer
    {

        public FeedTransformer( UniMap content )
        {
            _content = content;
        }

        @Override
        public UniMap transformContent( UniMap updated, UniMap current )
        {
            UniMap out = merge( updated, current );

            // FIXME: Means for prioritization based on _new/updated counts?
            // Filter based?

            return out;
        }

        @Override
        public UniMap transformReference( UniMap updated, UniMap current )
        {
            UniMap out = merge( updated, current );

            out.set( CURRENT, current );
            out.set( REFERER, _content );

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

        private UniMap _content;
        private int _newReferences = 0;
        private int _updatedReferences = 0;
    }

    @Override
    public List<FilterContainer> children()
    {
        return Arrays.asList( _updateRefFilter, _newRefFilter );
    }

    @Override
    public void close()
    {
        _updateRefFilter.close();
        _newRefFilter.close();
    }

    private static final List<Key> BASE_KEYS =
        Arrays.asList( new Key[] { UHASH, HOST, URL,
                                   TYPE,
                                   REF_PUB_DATE,
                                   PRIORITY,
                                   NEXT_VISIT_AFTER,
                                   REFERER } );

    private DataSource _dsource;
    private ContentMapper _mapper;

    private FilterContainer _updateRefFilter = new NoOpFilter();
    private FilterContainer _newRefFilter    = new NoOpFilter();

    protected static final Logger _log =
        LoggerFactory.getLogger( UpdateFilter.class );

}
