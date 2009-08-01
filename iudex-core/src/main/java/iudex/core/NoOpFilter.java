package iudex.core;

import com.gravitext.htmap.UniMap;

public class NoOpFilter implements ContentFilter
{
    @Override
    public boolean filter( UniMap content )
    {
        return true;
    }
}
