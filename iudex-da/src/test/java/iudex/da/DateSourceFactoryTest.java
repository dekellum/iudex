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


import iudex.core.VisitURL;
import iudex.da.DataSourceFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;


public class DateSourceFactoryTest
{
    private static DataSource _dataSource = null;

    @BeforeClass
    public static void Setup()
    {      
        _dataSource = DataSourceFactory.create();
    }
    
    @AfterClass
    public static void Close()
    {
    }
    
    @Test @Ignore
    public void testUpdate() throws SQLException, VisitURL.SyntaxException
    {
        VisitURL url = 
            VisitURL.normalize( "http://gravitext.com/blog/feed/atom.xml" );
        
        Object[] params = new Object[] {
            url.uhash(),
            url.toString(),
            url.host(),
            "FEED",
            1.1
        };
        Connection c = _dataSource.getConnection();
        c.setAutoCommit( true );
        try { 
            QueryRunner run = new QueryRunner();
            run.update( c,
                        "INSERT INTO urls (uhash, url, host, type, priority) " + 
                        "VALUES (?,?,?,?,?);", params );
        }
        finally {
            c.close();
        }
        
    }
    
    @Test
    public void testTemplate() throws SQLException
    {
        ResultSetHandler h = new ResultSetHandler() {
            public Object handle(ResultSet rs) throws SQLException 
            {
                while( rs.next() ) {
                    String gcid = rs.getString( "uhash");
                    String url = rs.getString( "url");
                    System.out.format( "%10s %40s\n", gcid, url );
                }

                return null;
            }
        };
        
        // Create a QueryRunner that will use connections from
        // the given DataSource
        QueryRunner run = new QueryRunner(_dataSource);

        // Execute the query and get the results back from the handler
        run.query( "SELECT * FROM urls", h );
    }
}
