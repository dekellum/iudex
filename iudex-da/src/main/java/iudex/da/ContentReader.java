package iudex.da;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import com.gravitext.htmap.UniMap;

public class ContentReader
{
    public ContentReader( DataSource source, ContentMapper mapper )
    {
        _mapper = mapper;
        _dsource = source;
    }

    public ContentMapper mapper()
    {
        return _mapper;
    }

    public DataSource dataSource()
    {
        return _dsource;
    }

    @SuppressWarnings("unchecked")
    public List<UniMap> select( String query, Object... params )
        throws SQLException
    {
        QueryRunner runner = new QueryRunner( _dsource );

        return (List<UniMap>) runner.query( query, new MapHandler(), params );
    }

    protected final class MapHandler implements ResultSetHandler
    {
        @Override
        public List<UniMap> handle( ResultSet rset ) throws SQLException
        {
            ArrayList<UniMap> contents = new ArrayList<UniMap>( 128 );
            while( rset.next() ) {
                contents.add( _mapper.fromResultSet( rset ) );
            }
            return contents;
        }

    }

    private final DataSource _dsource;
    private final ContentMapper _mapper;
}
