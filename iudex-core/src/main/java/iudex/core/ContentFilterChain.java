package iudex.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Closeable;

public class ContentFilterChain
    implements ContentFilterContainer, Described
{
    public ContentFilterChain( String description,
                               List<ContentFilter> filters )
    {
        _description = description;
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

    @Override
    public List<Object> describe()
    {
        return Arrays.asList( (Object) _description );
    }

    private final String _description;
    private final ArrayList<ContentFilter> _filters;
}
