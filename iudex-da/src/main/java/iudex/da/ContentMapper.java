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

import iudex.core.VisitURL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.KeySpace;
import com.gravitext.htmap.UniMap;

import static iudex.core.ContentKeys.*;

public final class ContentMapper
{
    // An alternative KEY_SPACE for "special" keys defined for purposes of
    // uniform database mapping (and shouldn't exist in UniMap).
    private static final KeySpace ALT_KEY_SPACE = new KeySpace();

    public static final Key<String> UHASH = 
        ALT_KEY_SPACE.create( "uhash", String.class );
    
    public static final Key<String> HOST = 
        ALT_KEY_SPACE.create( "host", String.class );
    
    public ContentMapper( List<Key> fields )
    {
        _fields = fields;
    }

    public ContentMapper( Key... fields ) 
    {
        this( Arrays.asList( fields ) );
    }
    
    public CharSequence fieldNames()
    {
        StringBuilder builder = new StringBuilder( 64 );
        appendFieldNames( builder );
        return builder;
    }
    
    public void appendFieldNames( StringBuilder out )
    {
        boolean first = true;
        for( Key<?> key : _fields ) {
            if( first ) first = false;
            else out.append( ", " );
            out.append( key.name() );
        }
    }
    
    public void appendQArray( StringBuilder out )
    {
        boolean first = true;
        final int len = _fields.size();
        for( int i = 0; i < len; ++i ) {
            if( first ) first = false;
            else out.append( ',' );
            out.append( '?' );
        }
    }
    
    public UniMap fromResultSet( ResultSet rset ) throws SQLException
    {
        UniMap content = new UniMap();
        int i = 1;
        for( Key<?> key : _fields ) {
            if( key == URL ) {
                content.set( URL, VisitURL.trust( rset.getString( i ) ) );
            }
            else if( ( key == UHASH ) || ( key == HOST ) ){
                // ignore on input
            }
            else {
                // FIXME: intern type,status,reason strings? { "x".intern(); }
                content.put( key, rset.getObject( i ) );
            }
            i++;
        }
        
        return content;
    }
    
    public void updateResultSet( UniMap content, ResultSet rset ) 
        throws SQLException
    {
        for( Key<?> key : _fields ) {
            final String name = key.name();
            
            if( key == URL ) {
                //NPE on missing URL (required)
                rset.updateString( name, content.get( URL ).toString() );
            }
            else if( key == UHASH ) {
                rset.updateString( name, content.get( URL ).uhash() );
            }
            else if( key == HOST ) {
                rset.updateString( name, content.get( URL ).host() );
            }
            else {
                rset.updateObject( name, content.get( key ) );
                //NULL ok, at least with PostgreSQL
            }

        }
    }
    
    public void toStatement( UniMap content, PreparedStatement stmt )
        throws SQLException
    {
        int i = 1;
        for( Key<?> key : _fields ) {
            if( key == URL ) {
                //NPE on missing URL (required)
                stmt.setString( i, content.get( URL ).toString() );
            }
            else if( key == UHASH ) {
                stmt.setString( i, content.get( URL ).uhash() );
            }
            else if( key == HOST ) {
                stmt.setString( i, content.get( URL ).host() );
            }
            else {
                if( key.valueType().equals( java.util.Date.class ) ) {
                    stmt.setTimestamp( i, 
                        convertDate( (java.util.Date) content.get( key ) ) );
                }
                else {
                    stmt.setObject( i, content.get( key ) );
                }
                //NULL ok, at least with PostgreSQL
            }
            
            i++;
        }
    }
    private static final Timestamp convertDate( java.util.Date date ) {
        return ( date == null ) ? null : new Timestamp( date.getTime() );
    }
    

    private final List<Key> _fields;

}
