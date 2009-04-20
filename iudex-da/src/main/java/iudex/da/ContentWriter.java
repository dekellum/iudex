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

import iudex.core.Content;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import com.gravitext.htmap.Key;

public class ContentWriter
{
    public ContentWriter( DataSource source )
    {
        _dsource = source;
    }
    public int write( Iterable<Content> contents ) 
        throws SQLException
    {
        StringBuilder sql = new StringBuilder(128);
        sql.append( "INSERT INTO urls (" );
        POLL_MAPPER.appendFieldNames( sql );
        sql.append( ") VALUES (" );
        POLL_MAPPER.appendQArray( sql );
        sql.append( ");" );
        
        Connection conn = _dsource.getConnection();
        conn.setAutoCommit( false );
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement( sql.toString() );

            for( Content content : contents ) {
                POLL_MAPPER.toStatement( content, statement );
                statement.addBatch();
            }
            int[] rows = statement.executeBatch();
            
            conn.commit();
            
            int sum = 0;
            for( int row : rows ) {
                sum += row;
            }
            return sum;
        } 
        finally {
            if( statement != null ) statement.close();
        }
    }
    
    //FIXME: Set from ruby for extensibility
    private static final ContentMapper POLL_MAPPER =  
        new ContentMapper( Arrays.asList( new Key<?>[] {
            UHASH,
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
            NEXT_VISIT_AFTER } ) );
    
    private final DataSource _dsource;
}
