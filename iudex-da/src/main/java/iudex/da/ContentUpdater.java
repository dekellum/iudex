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
     * Set max number of retries, not including the initial try.
     * Default: 3
     */
    public void setMaxRetries( int count )
    {
        _maxRetries = count;
    }

    public void setUpdateContent( boolean doUpdateContent )
    {
        _doUpdateContent = doUpdateContent;
    }

    public void setUpdateReferer( boolean doUpdateReferer )
    {
        _doUpdateReferer = doUpdateReferer;
    }

    public void setUpdateReferences( boolean doUpdateReferences )
    {
        _doUpdateReferences = doUpdateReferences;
    }

    /**
     * Update content REFERENCES, REFERER, and then the content itself.
     *
     * Any SQLExceptions that returns true from
     * {@link #handleError(int, SQLException)} will result in retries up to
     * maxRetries. When not (including when maxRetries is exceeded) the last
     * SQLException is re-thrown to indicate the failure. In all cases these
     * exceptions are logged here.
     *
     * @throws SQLException when not handled or out of retries.
     */
    public void update( UniMap content ) throws SQLException
    {
        Connection conn = dataSource().getConnection();
        try {
            conn.setAutoCommit( false );
            conn.setTransactionIsolation( isolationLevel() );

            int tries = 0;
            retry: while( true ) {
                try {
                    ++tries;

                    List<UniMap> refs = content.get( ContentKeys.REFERENCES );
                    if( refs != null && _doUpdateReferences ) {
                        update( refs, conn );
                    }

                    UniMap referer = content.get( ContentKeys.REFERER );
                    if( referer != null && _doUpdateReferer ) {
                        update( referer, conn, true );
                    }

                    if( _doUpdateContent ) {
                        update( content, conn, false );
                    }

                    conn.commit();

                    break retry;
                }
                catch( SQLException x ) {
                    if( handleError( tries, x ) ) {
                        conn.rollback();
                        continue retry;
                    }
                    throw x;
                }
            }

            if( tries > 1 ) {
                _log.info( "Update succeeded only after {} attempts", tries );
            }
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
            conn.setTransactionIsolation( isolationLevel() );

            int tries = 0;
            retry: while( true ) {
                try {
                    ++tries;

                    update( references, conn );

                    conn.commit();

                    break retry;
                }
                catch( SQLException x ) {
                    if( handleError( tries, x ) ) {
                        conn.rollback();
                        continue retry;
                    }
                    throw x;
                }
            }

            if( tries > 1 ) {
                _log.info( "Update succeeded only after {} attempts", tries );
            }
        }
        finally {
            if( conn != null ) conn.close();
        }
    }

    protected Transformer transformer()
    {
        return _transformer;
    }

    /**
     * Return true if a retry should be made, by inspection of the SQLException
     * and number of tries already attempted. Log accordingly. Override to reset
     * any state before a retry.
     */
    protected boolean handleError( int tries, SQLException x )
    {
        if( tries <= _maxRetries ) {
            SQLException s = x;
            while( s != null ) {
                String state = s.getSQLState();
                // Retry PostgreSQL Unique Key (i.e. uhash) violation or any
                // Transaction Rollback
                if( ( state != null ) &&
                    ( state.equals( "23505" ) ||
                      state.startsWith( "40" ) ) ) {
                    _log.debug( "Retry {} after: ({}) {}",
                                tries, state, s.getMessage() );
                    return true;
                }
                s = s.getNextException();
            }
        }

        SQLException s = x;
        while( s != null ) {
            _log.error( "Last try {}: ({}) {}",
                        tries, s.getSQLState(), s.getMessage() );
            s = s.getNextException();
        }

        return false;
    }

    protected void update( UniMap content, Connection conn )
        throws SQLException
    {
        update( content, conn, false );
    }

    protected void update( UniMap content, Connection conn, boolean isReferer )
        throws SQLException
    {
        final StringBuilder qb = new StringBuilder( 256 );
        qb.append( "SELECT " );
        mapper().appendFieldNames( qb );
        qb.append( " FROM urls WHERE uhash = ? FOR UPDATE;" );

        UpdateQueryRunner runner = new UpdateQueryRunner();
        int update =
            runner.query( conn, qb.toString(),
                          new OneUpdateHandler( content, conn, isReferer ),
                          content.get( ContentKeys.URL ).uhash() );

        if( update == 0 ) {

            UniMap out = null;
            if( isReferer ) {
                out = _transformer.transformReferer( content, null );
            }
            else {
                out = _transformer.transformContent( content, null );
            }

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
                                 Connection connection,
                                 boolean isReferer )
        {
            _content = content;
            _connection = connection;
            _isReferer = isReferer;
        }

        @Override
        public Integer handle( ResultSet rs ) throws SQLException
        {
            if( rs.next() ) {
                final UniMap in = mapper().fromResultSet( rs );

                UniMap out = null;
                if( _isReferer ) {
                    out = _transformer.transformReferer( _content, in );
                }
                else {
                    out = _transformer.transformContent( _content, in );
                }
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
        private final Connection _connection;
        private final UniMap _content;
        private final boolean _isReferer;
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
    private int _maxRetries = 3;
    private boolean _doUpdateContent = true;
    private boolean _doUpdateReferer = true;
    private boolean _doUpdateReferences = true;
}
