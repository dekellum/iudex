/*
 * Copyright (c) 2008-2013 David Kellum
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

import iudex.util.LogWriter;

import java.io.PrintWriter;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Minimal DataSource for standalone junit testing
 */
public class DataSourceFactory
{

    public static DataSource create()
    {
        DataSourceFactory factory = new DataSourceFactory();
        HashMap<String,String> params = new HashMap<String,String>();
        params.put( "dsf.driver.class", "org.postgresql.Driver" );
        params.put( "dsf.uri", "jdbc:postgresql:iudex_test");
        params.put( "user", "iudex" );
        params.put( "loglevel", "2" );
        return factory.create( params );
    }

    public DataSource create( Map<String,String> params )
    {
        String className = params.remove( "dsf.driver.class" );
        if( className != null ) {
            try {
               Class<?> driverClass = Class.forName( className );
               assert( driverClass != null );
            }
            catch( ClassNotFoundException x ) {
                throw new RuntimeException( x );
            }
        }
        setLogWriter();

        String uri = params.remove( "dsf.uri" );

        Properties props = new Properties();
        for( Map.Entry<String, String> e : params.entrySet() ) {
            props.setProperty( e.getKey(), e.getValue() );
        }
        DriverManagerConnectionFactory conFactory =
            new DriverManagerConnectionFactory( uri, props );

        ObjectPool conPool = new GenericObjectPool(null);
        //min, max, etc. connections

        // Sets self on conPool
        new PoolableConnectionFactory( conFactory, conPool,
                                       null, //stmtPoolFactory
                                       null,
                                       false,
                                       true );

        return new PoolingDataSource( conPool );
    }

    public void setLogWriter()
    {
        LogWriter lw = new LogWriter( "iudex.da.driver" );
        lw.setRemovePattern(
           LOG_REMOVE_PATTERN );

        DriverManager.setLogWriter( new PrintWriter( lw, true ) );
    }

    private static final Pattern LOG_REMOVE_PATTERN =
        Pattern.compile(
            "(^\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d\\s\\(\\d\\)\\s)|(\\s+$)" );
}
