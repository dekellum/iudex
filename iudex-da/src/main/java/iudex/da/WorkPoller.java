package iudex.da;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

public class WorkPoller
{
    public WorkPoller( DataSource source )
    {
        _dsource = source;
    }

    public void poll() throws SQLException
    {
        QueryRunner runner = new QueryRunner( _dsource );
        runner.query( POLL_QUERY, 
                      new Object[] { new Date( System.currentTimeMillis() ), 
                                     URLS_PER_HOST, TOTAL_URLS }, 
                                     new PollHandler() );
        //FIXME: Use TimeStamp for date/time
    }

    private class PollHandler implements ResultSetHandler
    {
        @Override
        public Object handle( ResultSet rs ) throws SQLException
        {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    private static final String POLL_QUERY = 
        "SELECT url, host, type, priority " + 
        "FROM ( SELECT * FROM urls " +
        "       WHERE next_visit_after <= ? " + 
        "       AND uhash IN ( SELECT uhash FROM urls o " + 
        "                      WHERE o.host = urls.host " +
        "                      ORDER BY priority DESC LIMIT ? ) " +
        "       ORDER BY priority DESC LIMIT ? ) AS sub " +
        " ORDER BY host, priority DESC;"; 
    
    
    private final DataSource _dsource;
    private int URLS_PER_HOST = 5;
    private int TOTAL_URLS = 18;
}
