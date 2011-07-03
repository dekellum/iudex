package iudex.http;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ContentTypeSet
{
    /**
     * Special set matching any Content-Type (including unspecified).
     */
    @SuppressWarnings("unchecked")
    public static final ContentTypeSet ANY
        = new ContentTypeSet( Collections.EMPTY_SET );

    /**
     * Construct set from specified types or type/* patterns, which should
     * already be normalized (i.e. lower case, trimmed.)
     */
    public ContentTypeSet( Collection<String> types )
    {
        _set = new HashSet<String>( types );
    }

    public boolean contains( ContentType ctype )
    {
        boolean match = false;

        if( _set.isEmpty() ) {
            match = true;
        }
        else {
            if ( ctype != null ) {
                String mtype = ctype.type();

                if ( mtype != null ) {

                    int i = mtype.lastIndexOf( '/' );
                    if( i > 0 ) {
                        String wcard = mtype.subSequence( 0, i ) + "/*";
                        match = _set.contains( wcard );
                    }

                    if( !match ) {
                        match = _set.contains( mtype );
                    }
                }
            }
            else {
                match = _set.contains( "*/*" );
            }
        }

        return match;
    }

    private final Set<String> _set;
}
