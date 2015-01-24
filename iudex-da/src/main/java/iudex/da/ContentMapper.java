/*
 * Copyright (c) 2008-2015 David Kellum
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
import java.util.ArrayList;
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
     * Logical key DOMAIN mapped to URL->VisitURL.domain()
     */
    public static final Key<String> DOMAIN =
        LOGICAL_KEYS.create( "domain", String.class );

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
            else if( ( key == UHASH ) || ( key == DOMAIN ) ) {
                // ignore on input
            }
            else if( ( key == REFERER ) || ( key == REFERENT ) ) {
                String uhash = rset.getString( i );
                if( uhash != null ) {
                    UniMap ref = new UniMap();
                    ref.set( URL, VisitURL.fromHash( uhash ) );
                    content.put( key, ref );
                }
            }
            else if( key == TYPE ) {
                content.set( TYPE, rset.getString( i ).intern() );
            }
            else {
                content.put( key, rset.getObject( i ) );
            }
            i++;
        }

        return content;
    }

    public List<Key> findUpdateDiffs( UniMap in, UniMap out )
    {
        ArrayList<Key> diffs = new ArrayList<Key>( _fields.size() );

        for( Key<?> key : _fields ) {
            if( key.space() != LOGICAL_KEYS ) {
                if( ! equalOrNull( in.get( key ), out.get( key ) ) ) {
                    diffs.add( key );
                }
            }
        }
        return diffs;
    }

    public void toStatement( UniMap content, PreparedStatement stmt )
        throws SQLException
    {
        toStatement( content, stmt, _fields );
    }

    public void toStatement( UniMap content,
                             PreparedStatement stmt,
                             List<Key> fields )
        throws SQLException
    {
        int i = 1;
        for( Key<?> key : fields ) {
            if( ( key == URL ) || ( key == UHASH ) || ( key == DOMAIN ) ) {
                stmt.setString( i, convertURL( key, content.get( URL ) ) );
            }
            else if( ( key == REFERER ) || ( key == REFERENT ) ) {
                UniMap ref = (UniMap) content.get( key );
                stmt.setString( i, hashOrNull( ref ) );
            }
            else {
                if( key.valueType().equals( UniMap.class ) ) {
                    throw new IllegalStateException( "Key? " + key.toString() );
                }
                stmt.setObject( i, convert( key, content.get( key ) ) );
            }
            i++;
        }
    }

    private String hashOrNull( UniMap ref )
    {
        return ( ref != null ) ? ref.get( URL ).uhash() : null;
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
        if( key == DOMAIN ) return url.domain();

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
