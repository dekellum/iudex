package iudex.da;

import com.gravitext.htmap.UniMap;

public class BaseTransformer implements Transformer
{
    /**
     * {@inheritDoc}
     * This implementation uses {@link #merge()}.
     */
    public UniMap transformContent( UniMap updated, UniMap current )
    {
        return merge( updated, current );
    }

    /**
     * {@inheritDoc}
     * This implementation uses {@link #merge()}.
     */
    public UniMap transformReference( UniMap updated, UniMap current )
    {
        return merge( updated, current );
    }

    /**
     * If current is null, returns updated, else returns a clone of current with
     * updated merged.
     */
    protected UniMap merge( final UniMap updated, final UniMap current )
    {
        if( current == null ) return updated;

        // Merge updated to clone of current
        UniMap t = current.clone();
        t.putAll( updated );
        return t;
    }

}
