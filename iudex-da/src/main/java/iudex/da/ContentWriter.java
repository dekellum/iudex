/*
 * Copyright (c) 2008-2014 David Kellum
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

import static iudex.core.ContentKeys.*;
import static iudex.da.ContentMapper.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

public class ContentWriter
    extends ContentReader
{
    public ContentWriter( DataSource source, ContentMapper mapper )
    {
        super( source, mapper );

        for( Key req : REQUIRED_KEYS ) {
            if( ! mapper.fields().contains( req ) ) {
                throw new IllegalArgumentException(
                   "ContentWriter needs mapper with " + req + " included." );
            }
        }
    }

    public int write( Iterable<UniMap> contents ) throws SQLException
    {
        Connection conn = dataSource().getConnection();
        try {
            conn.setAutoCommit( false );
            conn.setTransactionIsolation( isolationLevel() );

            int count = write( contents, conn );
            conn.commit();
            return count;
        }
        catch( SQLException orig ) {
            logError( orig );
            throw orig;
        }
        finally {
            if( conn != null ) conn.close();
        }
    }

    public int write( UniMap content ) throws SQLException
    {
        Connection conn = dataSource().getConnection();
        try {
            conn.setAutoCommit( false );
            conn.setTransactionIsolation( isolationLevel() );

            return write( content, conn );
        }
        catch( SQLException orig ) {
            logError( orig );
            throw orig;
        }
        finally {
            if( conn != null ) conn.close();
        }
    }

    protected int write( UniMap content, Connection conn ) throws SQLException
    {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement( formatInsert() );
            mapper().toStatement( content, stmt );
            int count = stmt.executeUpdate();
            conn.commit();
            return count;
        }
        finally {
            if( stmt != null ) stmt.close();
        }
    }

    protected int write( Iterable<UniMap> contents, Connection conn )
        throws SQLException
    {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement( formatInsert() );

            for( UniMap content : contents ) {
                mapper().toStatement( content, stmt );
                stmt.addBatch();
                //FIXME: needed? : stmt.clearParameters();
            }
            int[] rows = stmt.executeBatch();

            int sum = 0;
            for( int row : rows ) {
                sum += row;
            }
            return sum;
        }
        finally {
            if( stmt != null ) stmt.close();
        }
    }

    private String formatInsert()
    {
        StringBuilder sql = new StringBuilder(128);
        sql.append( "INSERT INTO urls (" );
        mapper().appendFieldNames( sql );
        sql.append( ") VALUES (" );
        mapper().appendQArray( sql );
        sql.append( ");" );
        return sql.toString();
    }

    private static final List<Key> REQUIRED_KEYS =
        Arrays.asList( new Key[] { URL, UHASH, DOMAIN, TYPE } );
}
