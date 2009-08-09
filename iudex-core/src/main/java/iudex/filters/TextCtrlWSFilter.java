package iudex.filters;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import iudex.core.ContentFilter;
import iudex.util.Characters;

public class TextCtrlWSFilter implements ContentFilter
{
    TextCtrlWSFilter( Key<CharSequence> field )
    {
        _field = field;
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
