package iudex.da;

import static iudex.core.ContentKeys.LAST_VISIT;
import static iudex.core.ContentKeys.PRIORITY;
import static iudex.core.ContentKeys.REASON;
import static iudex.core.ContentKeys.STATUS;
import static iudex.core.ContentKeys.TYPE;
import static iudex.core.ContentKeys.URL;
import iudex.core.Content;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import com.gravitext.htmap.Key;

public class WorkPoller
{
    public WorkPoller( DataSource source )
    {
        _dsource = source;
    }

    @SuppressWarnings("unchecked")
    public List<Content> poll() throws SQLException
    {
        QueryRunner runner = new QueryRunner( _dsource );
        
        String query = String.format( POLL_QUERY, POLL_MAPPER.fieldNames() );

        Object[] params = new Object[] { _urlsPerHost, _totalUrls };
        
        return (List<Content>)
            runner.query( query, params, new PollHandler() );
    }

    private class PollHandler implements ResultSetHandler
    {
        @Override
        public List<Content> handle( ResultSet rset ) throws SQLException
        {
            ArrayList<Content> contents = new ArrayList<Content>( _totalUrls );
            while( rset.next() ) {
                contents.add( POLL_MAPPER.fromResultSet( rset ) );
            }
            return contents;
        }
        
    }
    
    private static final String POLL_QUERY = 
        "SELECT %s " + 
        "FROM ( SELECT * FROM urls " +
        "       WHERE (next_visit_after <= now() OR next_visit_after IS NULL) "+
        "       AND (status IS NULL OR status NOT IN ('REDIRECT', 'REJECT'))" +
        "       AND uhash IN ( SELECT uhash FROM urls o " + 
        "                      WHERE o.host = urls.host " +
        "                      ORDER BY priority DESC LIMIT ? ) " +
        "       ORDER BY priority DESC LIMIT ? ) AS sub " +
        "ORDER BY host, priority DESC;"; 
    // Don't include REDIRECT based on: always revisit referent.
    
    private static final ContentMapper POLL_MAPPER =  
        new ContentMapper( Arrays.asList( new Key<?>[] { 
            URL,
            TYPE,
            LAST_VISIT,
            STATUS,
            REASON,
            PRIORITY } ) );
    //FIXME: Set from ruby for extensibility
    
    private final DataSource _dsource;
    private int _urlsPerHost = 100;
    private int _totalUrls = 10000;
}
