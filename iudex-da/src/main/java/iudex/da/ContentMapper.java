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

/**
 * Automagical mapping between UniMap values and JDBC ResultSets and
 * Statements.
 */
public final class ContentMapper
{
    /**
     * An alternative key space for "logical" keys defined for
     * purposes of uniform database mapping, but which should not
     * exist directly in a UniMap instance.
     */
    public static final KeySpace LOGICAL_KEYS = new KeySpace();

    /**
     * Logical key UHASH mapped to URL->VisitURL.uhash()
     */
    public static final Key<String> UHASH =
        LOGICAL_KEYS.create( "uhash", String.class );

    /**
     * Logical key UHASH mapped to URL->VisitURL.host()
     */
    public static final Key<String> HOST =
        LOGICAL_KEYS.create( "host", String.class );

    public ContentMapper( List<Key> fields )
    {
        _fields = fields;
    }

    public ContentMapper( Key... fields )
    {
        this( Arrays.asList( fields ) );
    }

    public List<Key> fields()
    {
        return _fields;
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
            else if( ( key == UHASH ) || ( key == HOST ) ) {
                // ignore on input
            }
            else if( key == REFERER ) {
                String uhash = rset.getString( i );
                if( uhash != null ) {
                    UniMap ref = new UniMap();
                    ref.set( URL, VisitURL.fromHash( uhash ) );
                    content.set( REFERER, ref );
                }
            }
            else {
                // FIXME: intern type,status,reason strings? { "x".intern(); }
                content.put( key, rset.getObject( i ) );
            }
            i++;
        }

        return content;
    }

    public void update( ResultSet rset, UniMap out )
        throws SQLException
    {
        for( Key<?> key : _fields ) {
            update( rset, key, out );
        }
    }

    public void update( ResultSet rset, Key<?> key, UniMap out )
        throws SQLException
    {
        final String name = key.name();
        if( ( key == URL ) || ( key == UHASH ) || ( key == HOST ) ) {
            rset.updateString( name, convertURL( key, out.get( URL ) ) );
        }
        else {
            rset.updateObject( name, convert( key, out.get( key ) ) );
            // NULL ok, at least with PostgreSQL
        }
    }

    public boolean update( ResultSet rs, UniMap in, UniMap out )
        throws SQLException
    {
        boolean change = false;
        for( Key<?> key : _fields ) {
            if( key.space() != LOGICAL_KEYS ) {
                if( update( rs, key, in, out ) ) change = true;
            }
        }
        return change;
    }

    public boolean update( ResultSet rs, Key<?> key, UniMap in, UniMap out )
        throws SQLException
    {
        if( ! equalOrNull( in.get( key ), out.get( key ) ) ) {
            update( rs, key, out );
            return true;
        }
        return false;
    }

    public void toStatement( UniMap content, PreparedStatement stmt )
        throws SQLException
    {
        int i = 1;
        for( Key<?> key : _fields ) {
            if( ( key == URL ) || ( key == UHASH ) || ( key == HOST ) ) {
                stmt.setString( i, convertURL( key, content.get( URL ) ) );
            }
            else if( key == REFERER ) {
                UniMap ref = content.get( REFERER );
                stmt.setString( i,
                                (ref != null) ? ref.get( URL ).uhash() : null );
            }
            else {
                stmt.setObject( i, convert( key, content.get( key ) ) );
            }
            i++;
        }
    }

    private Object convert( Key<?> key, Object value )
    {
        if( key.valueType().equals( java.util.Date.class ) ) {
            return convertDate( (java.util.Date) value );
        }
        if( key.valueType().equals( java.lang.CharSequence.class ) ) {
            return ( value != null ) ? value.toString() : null;
        }
        return value;
    }

    private String convertURL( Key<?> key, VisitURL url )
    {
        if( key == URL   ) return url.toString();
        if( key == UHASH ) return url.uhash();
        if( key == HOST  ) return url.host();

        throw new IllegalArgumentException
            ( "Non URL derived logical key: " + key );
    }

    private static final Timestamp convertDate( java.util.Date date )
    {
        return ( date == null ) ? null : new Timestamp( date.getTime() );
    }

    private static boolean equalOrNull( Object first, Object second )
    {
        return ( ( first == second ) ||
                 ( ( first != null ) && equalsDateAware( first, second ) ) );
    }

    private static boolean equalsDateAware( Object first, Object second )
    {
        // Needed to avoid false negative from util.Date, sql.Date/Timestamp
        // incompatibility.
        if( first instanceof java.util.Date ) {
            if( second instanceof java.util.Date ) {
                return ( ((java.util.Date)first).getTime() ==
                         ((java.util.Date)second).getTime() );
            }
        }
        return first.equals( second );
    }

    private final List<Key> _fields;
}
