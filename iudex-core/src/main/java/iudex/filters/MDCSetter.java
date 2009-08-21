package iudex.filters;

import org.slf4j.MDC;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import iudex.core.Filter;

public class MDCSetter implements Filter
{

    MDCSetter( Key field )
    {
        _field = field;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean filter( UniMap content )
    {
        Object value = content.get( _field );
        MDC.put( _field.name(), ( value != null ) ? value.toString() : "" );
        return true;
    }

    private Key _field;
}
