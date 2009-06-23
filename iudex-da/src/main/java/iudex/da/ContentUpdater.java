package iudex.da;

import static iudex.core.ContentKeys.*;
import static iudex.da.ContentMapper.*;

import iudex.core.ContentKeys;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

public class ContentUpdater
    extends ContentWriter
{
    public ContentUpdater( DataSource source, ContentMapper mapper )
    {
        super( source, mapper );
    }

    //FIXME: Transform interface as param?
    public void update( List<UniMap> references ) throws SQLException
    {
        final HashMap<String,UniMap> hashes = 
            new HashMap<String,UniMap>( references.size() );

        final String qry = formatSelect( references, hashes );
        
        Connection conn = dataSource().getConnection();
        try { 
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            //FIXME: Correct isolation?
            conn.setAutoCommit( false );

            UpdateQueryRunner runner = new UpdateQueryRunner();
            runner.query( conn, qry, new UpdateHandler( hashes ) );
            
            final ArrayList<UniMap> remains = 
                new ArrayList<UniMap>( hashes.size() );
            for( UniMap rem : hashes.values() ) {
                 remains.add( transform( rem, null ) );
            }

            write( remains, conn );

            conn.commit();
        }
        finally {
            if( conn != null ) conn.close();
        }
    }

    private String formatSelect( final List<UniMap> references,
                                 final HashMap<String, UniMap> hashes )
    {
        //FIXME: Pass in hashes as input for values of "IN" clause
        
        final StringBuilder qb = new StringBuilder( 512 );
        qb.append( "SELECT " );
        mapper().appendFieldNames( qb );
        qb.append( " FROM urls WHERE uhash IN (" );
        boolean first = true;
        for( UniMap r : references ) {
            if( first ) first = false; 
            else qb.append( ", " );
            
            final String uhash = r.get( ContentKeys.URL ).uhash();
            hashes.put( uhash, r );
            
            qb.append( '\'' );
            qb.append( uhash );
            qb.append( '\'' );
        }
        qb.append( ");" );

        return qb.toString();
    }
    
    
    private UniMap transform( UniMap ref, UniMap found )
    {
        if( found == null ) return ref;
        
        UniMap t = found.clone();
        t.putAll( ref );
        return t;
    }
    
    private final class UpdateHandler implements ResultSetHandler
    {

        public UpdateHandler( HashMap<String, UniMap> hashes )
        {
            _hashes = hashes;
        }

        @Override
        public Object handle( ResultSet rs ) throws SQLException
        {
            while( rs.next() ) {
                UniMap found = mapper().fromResultSet( rs );
                String uhash = rs.getString( "uhash" );
                UniMap ref = _hashes.remove( uhash );
                UniMap out = transform( ref, found );
                //FIXME: Generalize based on out, transform
                rs.updateObject( "priority", 22.2F ); //FIXME: Testing
                rs.updateRow();
            }
            return null;
        }
        

        private HashMap<String, UniMap> _hashes;
    }

    
    private static final class UpdateQueryRunner extends QueryRunner
    {
        @Override
        protected PreparedStatement prepareStatement( Connection conn,
                                                      String sql )
            throws SQLException
        {
            return conn.prepareStatement( sql, 
                                          ResultSet.TYPE_FORWARD_ONLY, 
                                          ResultSet.CONCUR_UPDATABLE );
        }
    }
    
    //FIXME: Set from ruby for extensibility
    /*
    private static final ContentMapper UPDATE_MAPPER =  
        new ContentMapper( UHASH,
                           TYPE, 
                           STATUS,
                           REASON,
                           TITLE,
                           REF_PUB_DATE,
                           REFERER,
                           PRIORITY,
                           NEXT_VISIT_AFTER );
    */

}
