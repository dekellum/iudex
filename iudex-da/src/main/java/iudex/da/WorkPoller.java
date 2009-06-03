/*
 * Copyright (C) 2008-2009 David Kellum
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

package iudex.da;

import static iudex.core.ContentKeys.LAST_VISIT;
import static iudex.core.ContentKeys.PRIORITY;
import static iudex.core.ContentKeys.REASON;
import static iudex.core.ContentKeys.STATUS;
import static iudex.core.ContentKeys.TYPE;
import static iudex.core.ContentKeys.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

public class WorkPoller
{
    public WorkPoller( DataSource source )
    {
        _dsource = source;
    }

    @SuppressWarnings("unchecked")
    public List<UniMap> poll() throws SQLException
    {
        QueryRunner runner = new QueryRunner( _dsource );
        
        String query = String.format( POLL_QUERY, POLL_MAPPER.fieldNames() );

        Object[] params = new Object[] { _urlsPerHost, _totalUrls };
        
        return (List<UniMap>)
            runner.query( query, params, new PollHandler() );
    }

    private class PollHandler implements ResultSetHandler
    {
        @Override
        public List<UniMap> handle( ResultSet rset ) throws SQLException
        {
            ArrayList<UniMap> contents = new ArrayList<UniMap>( _totalUrls );
            while( rset.next() ) {
                contents.add( POLL_MAPPER.fromResultSet( rset ) );
            }
            return contents;
        }
        
    }
    
    private static final String POLL_QUERY = 
        "SELECT %s " + 
        "FROM ( SELECT * FROM urls " +
        "       WHERE next_visit_after <= now() " +
        "       AND uhash IN ( SELECT uhash FROM urls o " + 
        "                      WHERE o.host = urls.host " +
        "                      ORDER BY priority DESC LIMIT ? ) " +
        "       ORDER BY priority DESC LIMIT ? ) AS sub " +
        "ORDER BY host, priority DESC;"; 
    
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
