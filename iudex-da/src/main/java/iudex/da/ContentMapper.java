package iudex.da;

import iudex.core.Content;
import iudex.core.ContentKeys;
import iudex.core.VisitURL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.KeySpace;

import static iudex.core.ContentKeys.*;

public final class ContentMapper
{
    public ContentMapper( List<Key<?>> fields )
    {
        _fields = fields;
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
    
    public Content fromResultSet( ResultSet rset ) throws SQLException
    {
        Content content = new Content();
        int i = 1;
        for( Key<?> key : _fields ) {
            if( key == ContentKeys.URL ) {
                content.set( ContentKeys.URL, 
                             VisitURL.trust( rset.getString( i ) ) );
            }
            else {
                // FIXME: intern type,status,reason strings? { "x".intern(); }

                content.put( key, rset.getObject( i ) );
            }
            i++;
        }
        
        return content;
    }

    public void toStatement( Content content, PreparedStatement statement ) 
        throws SQLException
    {
        int i = 1;
        for( Key<?> key : _fields ) {
            if( key == URL ) {
                //NPE on missing URL (required)
                statement.setString( i, content.get( URL ).toString() );
            }
            else if( key == UHASH ) {
                statement.setString( i, content.get( URL ).uhash() );
            }
            else if( key == HOST ) {
                statement.setString( i, content.get( URL ).host() );
            }
            else {
                statement.setObject( i, content.get( key ) );
                //Null should be ok.
            }
            
            i++;
        }
    }

    private static final KeySpace ALT_KEY_SPACE = new KeySpace();

    static final Key<String> UHASH = 
        ALT_KEY_SPACE.create( "uhash", String.class );
    
    static final Key<String> HOST = 
        ALT_KEY_SPACE.create( "host", String.class );

    private final List<Key<?>> _fields;

}
