/*
 * Copyright (c) 2008-2010 David Kellum
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

import iudex.barc.BARCDirectory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

public class CacheFileUpdater
{
    public CacheFileUpdater( DataSource source )
    {
        _dataSource = source;
    }

    public DataSource dataSource()
    {
        return _dataSource;
    }

    public void update() throws SQLException
    {
        Connection conn = dataSource().getConnection();
        try {
            conn.setAutoCommit( false );
            conn.setTransactionIsolation(
                Connection.TRANSACTION_READ_COMMITTED );

            String qry =
                "SELECT uhash, cache_file, cache_file_offset " +
                "FROM urls " +
                "WHERE cache_file IS NOT NULL " +
                "      AND cache_file_offset IS NOT NULL " +
                "ORDER BY cache_file, cache_file_offset " +
                "FOR UPDATE;";

            final UpdateQueryRunner runner = new UpdateQueryRunner();
            runner.query( conn, qry, new UpdateHandler() );

            conn.commit();
        }
        finally {
            if( conn != null ) conn.close();
        }
    }

    private final class UpdateHandler
        implements ResultSetHandler<Object>
    {
        public UpdateHandler( BARCDirectory input, BARCDirectory output )
        {
            _inDir = input;
            _outDir = output;
        }

        @Override
        public Object handle( ResultSet rs ) throws SQLException
        {
            while( rs.next() ) {
                String uhash = rs.getString( 0 );
                int cfile    = rs.getInt( 1 );
                long coffset = rs.getLong( 2 );

                rs.updateRow();
            }
            return null;
        }

        private final BARCDirectory _inDir;
        private final BARCDirectory _outDir;
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

    private DataSource _dataSource;
}
