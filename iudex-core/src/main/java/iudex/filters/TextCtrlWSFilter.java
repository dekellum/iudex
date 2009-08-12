package iudex.filters;

import java.util.Arrays;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import iudex.core.ContentFilter;
import iudex.core.Described;
import iudex.util.Characters;

public final class TextCtrlWSFilter
    implements ContentFilter, Described
{
    public TextCtrlWSFilter( Key<CharSequence> field )
    {
        _field = field;
    }

    @Override
    public List<Object> describe()
    {
        return Arrays.asList( (Object) _field );
    }

    @Override
    public boolean filter( UniMap content )
    {
        CharSequence in = content.get( _field );
        if( in != null ) {
            CharSequence out = Characters.cleanCtrlWS( in );
            if( out.length() == 0 ) out = null;
            content.set( _field, out );
        }

        return true;
    }

    private final Key<CharSequence> _field;
}
