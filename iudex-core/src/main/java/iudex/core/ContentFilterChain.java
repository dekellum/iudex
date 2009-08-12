package iudex.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Closeable;

public class ContentFilterChain
    implements ContentFilterContainer
{
    public ContentFilterChain( List<ContentFilter> filters )
    {
        _filters = new ArrayList<ContentFilter>( filters );
    }

    public boolean filter( UniMap content )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<ContentFilter> children()
    {
        return Collections.unmodifiableList( _filters );
    }

    @Override
    public void close()
    {
        for( ContentFilter f : _filters ) {
            if( f instanceof Closeable ) ( (Closeable) f ).close();
        }
    }

    private final ArrayList<ContentFilter> _filters;
}
