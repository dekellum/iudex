package iudex.core;

import java.util.List;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Closeable;

public interface ContentFilterContainer
    extends ContentFilter, Closeable
{
    /**
     * {@inheritDoc}
     * Containers can not throw FilterException.
     */
    @Override
    public boolean filter( UniMap content );

    public List<ContentFilter> children();
}
