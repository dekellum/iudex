package iudex.core;

import com.gravitext.htmap.Key;

public class ContentKeys
{
    private static <T> Key<T> create( String name, Class<T> valueType )
    {
        return Content.KEY_SPACE.create( name, valueType );
    }

    public static final Key<VisitURL> URL = 
        create( "URL", VisitURL.class ); 

    public static final Key<CharSequence> TITLE = 
        create( "TITLE", CharSequence.class );
    
    public static final Key<ContentSource> CONTENT =
        create( "CONTENT", ContentSource.class );
}
