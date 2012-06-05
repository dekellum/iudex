/*
 * Copyright (c) 2008-2012 David Kellum
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

import iudex.core.ContentKeys;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
    public ContentUpdater( DataSource source,
                           ContentMapper mapper,
                           Transformer transformer )
    {
        super( source, mapper );
        _transformer = transformer;
    }

    /**
     * Update first any content REFERENCES and then the content itself.
     */
    public void update( UniMap content )
        throws SQLException
    {
        Connection conn = dataSource().getConnection();
        try {
            conn.setAutoCommit( false );
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            //FIXME: Correct isolation?

            List<UniMap> refs = content.get( ContentKeys.REFERENCES );
            if( refs != null ) {
                update( refs, conn );
            }

            UniMap referer = content.get( ContentKeys.REFERER );
            if( referer != null ) {
                //FIXME: Really sufficient as same path as content?
                update( referer, conn );
            }

            update( content, conn );

            conn.commit();
        }
        finally {
            if( conn != null ) conn.close();
        }
    }

    public void update( List<UniMap> references ) throws SQLException
    {
        Connection conn = dataSource().getConnection();
        try {
            conn.setAutoCommit( false );
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            update( references, conn );

            conn.commit();
        }
        finally {
            if( conn != null ) conn.close();
        }
    }

    protected void update( UniMap content, Connection conn )
        throws SQLException
    {
        final StringBuilder qb = new StringBuilder( 256 );
        qb.append( "SELECT " );
        mapper().appendFieldNames( qb );
        qb.append( " FROM urls WHERE uhash = ? FOR UPDATE;" );

        UpdateQueryRunner runner = new UpdateQueryRunner();
        int update = runner.query( conn, qb.toString(),
                                   new OneUpdateHandler( content, conn ),
                                   content.get( ContentKeys.URL ).uhash() );

        if( update == 0 ) {
            UniMap out = _transformer.transformContent( content, null );
            if( out != null ) write( out, conn );
        }
    }

    protected void update( UniMap content, List<Key> diff, Connection conn )
        throws SQLException
    {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement( formatUpdate( diff ) );
            mapper().toStatement( content, stmt, diff );
            stmt.setString( diff.size() + 1,
                            content.get( ContentKeys.URL ).uhash() );
            stmt.executeUpdate();
        }
        finally {
            if( stmt != null ) stmt.close();
        }
    }

    protected void update( List<UniMap> references, Connection conn )
        throws SQLException
    {
        final HashMap<String,UniMap> uhashes =
            new HashMap<String,UniMap>( references.size() );
        final String qry = formatSelect( references, uhashes );
        final UpdateQueryRunner runner = new UpdateQueryRunner();
        runner.query( conn, qry, new RefUpdateHandler( uhashes, conn ) );

        final ArrayList<UniMap> remains =
            new ArrayList<UniMap>( uhashes.size() );
        for( UniMap rem : uhashes.values() ) {
            UniMap out = _transformer.transformReference( rem, null );
            if( out != null ) remains.add( out );
        }
        if( remains.size() > 0 ) write( remains, conn );
    }

    private String formatSelect( final List<UniMap> references,
                                 final HashMap<String, UniMap> uhashes )
    {
        final StringBuilder qb = new StringBuilder( 512 );
        qb.append( "SELECT " );
        mapper().appendFieldNames( qb );
        qb.append( " FROM urls WHERE uhash IN (" );
        boolean first = true;
        for( UniMap r : references ) {
            if( first ) first = false;
            else qb.append( ", " );

            final String uhash = r.get( ContentKeys.URL ).uhash();
            uhashes.put( uhash, r );

            qb.append( '\'' );
            qb.append( uhash );
            qb.append( '\'' );
        }
        qb.append( ") FOR UPDATE;" );

        return qb.toString();
    }

    private String formatUpdate( List<Key> fields )
    {
        StringBuilder sql = new StringBuilder(128);
        boolean first = true;
        sql.append( "UPDATE urls SET " );
        for( Key<?> key : fields ) {
            if( first ) first = false;
            else sql.append( ", " );
            sql.append( key.name() ).append( " = ?" );
        }
        sql.append( " WHERE uhash = ?;" );
        return sql.toString();
    }

    private final class RefUpdateHandler
        implements ResultSetHandler<Object>
    {
        public RefUpdateHandler( HashMap<String, UniMap> hashes,
                                 Connection connection )
        {
            _hashes = hashes;
            _connection = connection;
        }

        @Override
        public Object handle( ResultSet rs ) throws SQLException
        {
            while( rs.next() ) {
                final UniMap in = mapper().fromResultSet( rs );
                final UniMap ref = _hashes.remove( rs.getString( "uhash" ) );

                UniMap out = _transformer.transformReference( ref, in );
                if( out != null ) {
                    List<Key> diff = mapper().findUpdateDiffs( in, out );
                    if( diff.size() > 0 ) {
                        update( out, diff, _connection );
                    }
                }
            }
            return null;
        }

        private Connection _connection;
        private HashMap<String, UniMap> _hashes;
    }

    private final class OneUpdateHandler
        implements ResultSetHandler<Integer>
    {
        public OneUpdateHandler( UniMap content,
                                 Connection connection )
        {
            _content = content;
            _connection = connection;
        }

        @Override
        public Integer handle( ResultSet rs ) throws SQLException
        {
            if( rs.next() ) {
                final UniMap in = mapper().fromResultSet( rs );

                UniMap out = _transformer.transformContent( _content, in );

                if( out != null ) {
                    List<Key> diff = mapper().findUpdateDiffs( in, out );
                    if( diff.size() > 0 ) {
                        update( out, diff, _connection );
                    }
                }

                return 1;
            }
            if( rs.next() ) {
                throw new IllegalStateException(
                   "Multiple url rows returned for uhash=" +
                   _content.get( ContentKeys.URL ).uhash() );
            }
            return 0;
        }
        private Connection _connection;
        private UniMap _content;
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
    private final Transformer _transformer;
}
