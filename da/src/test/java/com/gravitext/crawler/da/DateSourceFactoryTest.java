package com.gravitext.crawler.da;


import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.gravitext.crawler.URLHash;

import static org.junit.Assert.*;

public class DateSourceFactoryTest
{
    private static DataSource _dataSource = null;

    @BeforeClass
    public static void Setup()
    {      
        DataSourceFactory factory = new DataSourceFactory();
        HashMap<String,String> params = new HashMap<String,String>();
        params.put( "dsf.driver.class", "org.postgresql.Driver" );
        params.put( "dsf.uri", "jdbc:postgresql:crawler_test");
        params.put( "user", "david" );
        //params.put( "loglevel", "2" );
        _dataSource = factory.create( params );
        
        
    }
    
    @AfterClass
    public static void Close()
    {
    }
    @Test
    public void testUpdate() throws SQLException, URISyntaxException
    {
        URI uri = new URI( "http://gravitext.com/blog/feed/atom.xml" );
        uri = URLHash.normalize( uri );
        
        Object[] params = new Object[] {
            URLHash.hashURL( uri.toString() ).toString(),
            uri.toString(),
            uri.getHost(),
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
