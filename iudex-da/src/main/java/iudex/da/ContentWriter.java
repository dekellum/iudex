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

import static iudex.core.ContentKeys.*;
import static iudex.da.ContentMapper.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.UniMap;

public class ContentWriter
{
    public ContentWriter( DataSource source, ContentMapper mapper )
    {
        _dataSource = source;
        _mapper = mapper;
    }
   
    public ContentMapper mapper()
    {
        return _mapper;
    }

    public DataSource dataSource()
    {
        return _dataSource;
    }
    
    public int write( Iterable<UniMap> contents ) 
        throws SQLException
    {
        Connection conn = _dataSource.getConnection();
        try {
            conn.setAutoCommit( false );
            return write( contents, conn );
        }
        catch( SQLException orig ) {
            _log.error( "On write: " + orig.getMessage() );
            SQLException x = orig;
            while( ( x = x.getNextException() ) != null ) {
                _log.error( x.getMessage() );
            }
            throw orig;
        }
        finally {
            if( conn != null ) conn.close();
        }
    }

    protected int write( Iterable<UniMap> contents, 
                         Connection conn )
        throws SQLException
    {
        StringBuilder sql = new StringBuilder(128);
        sql.append( "INSERT INTO urls (" );
        mapper().appendFieldNames( sql );
        sql.append( ") VALUES (" );
        mapper().appendQArray( sql );
        sql.append( ");" );
        

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement( sql.toString() );

            for( UniMap content : contents ) {
                mapper().toStatement( content, stmt );
                stmt.addBatch();
            }
            int[] rows = stmt.executeBatch();
            
            conn.commit();
            
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
    
    //FIXME: Set from ruby for extensibility
    /*
    private static final ContentMapper POLL_MAPPER =  
        new ContentMapper( UHASH,
                           URL,
                           HOST,
                           TYPE,
                           LAST_VISIT,
                           STATUS,
                           REASON,
                           TITLE,
                           PUB_DATE,
                           REF_PUB_DATE,
                           PRIORITY,
                           NEXT_VISIT_AFTER );
                           */
    
    private final DataSource _dataSource;
    private final ContentMapper _mapper;
    
    protected static final Logger _log = 
        LoggerFactory.getLogger( ContentWriter.class  );
}
