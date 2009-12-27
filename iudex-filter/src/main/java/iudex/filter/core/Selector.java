package iudex.filter.core;

import iudex.filter.Described;
import iudex.filter.Filter;

import java.util.Arrays;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

/**
 * Selects content if field value equals a given value.
 * @param <VT> the selection key value type
 */
public class Selector<VT> implements Filter, Described
{
    public Selector( Key<VT> selectionKey, VT value )
    {
        _key = selectionKey;
        _value = value;
    }

    @Override
    public List<?> describe()
    {
        return Arrays.asList( _key.toString(), String.valueOf( _value ) );
    }

    /**
     * {@inheritDoc}
     * @return true if content key value equals the provided value.
     */
    @Override
    public boolean filter( UniMap content )
    {
        return _value.equals( content.get( _key ) );
    }

    private final Key<VT> _key;
    private final VT _value;
}
