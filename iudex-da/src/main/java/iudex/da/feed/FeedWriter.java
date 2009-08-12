package iudex.da.feed;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import javax.sql.DataSource;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import iudex.core.Filter;
import iudex.core.FilterContainer;
import iudex.da.BaseTransformer;
import iudex.da.ContentMapper;
import iudex.da.ContentUpdater;
import iudex.filters.NoOpFilter;

import static iudex.core.ContentKeys.*;
import static iudex.da.ContentMapper.*;

public class FeedWriter implements FilterContainer
{
    public FeedWriter( DataSource source,
                       List<Key> fields )
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
    public List<Filter> children()
    {
        return Arrays.asList(
            new Filter[] { this._updateRefFilter,
                                  this._newRefFilter } );
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
}
