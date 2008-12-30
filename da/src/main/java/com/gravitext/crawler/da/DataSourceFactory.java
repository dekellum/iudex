package com.gravitext.crawler.da;

import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

//FIXME: Best if all of this in ruby?
public class DataSourceFactory
{
    public DataSource create( Map<String,String> params )
    {
        String className = params.remove( "dsf.driver.class" );
        if( className != null ) {
            try {
               assert( Class.forName( className ) != null );
               System.out.println( "Success with: " + className );
            }
            catch( ClassNotFoundException x ) {
                throw new RuntimeException( x );
            }
        }
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
                                       params.get( "dsf.validation.query" ), 
                                       params.containsKey( "dsf.default.read.only" ),
                                       params.containsKey( "dsf.default.auto.commit" ) );
        
        return new PoolingDataSource( conPool );
    }
}
