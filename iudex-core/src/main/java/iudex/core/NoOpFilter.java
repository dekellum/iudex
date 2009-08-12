package iudex.core;

import java.util.Collections;
import java.util.List;

import com.gravitext.htmap.UniMap;

public class NoOpFilter implements ContentFilterContainer
{
    @Override
    public boolean filter( UniMap content )
    {
        return true;
    }

    @Override
    public List<ContentFilter> children()
    {
        return Collections.emptyList();
    }

    @Override
    public void close()
    {
    }
}
