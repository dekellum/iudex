package iudex.core.filters;

import static iudex.core.ContentKeys.URL;
import iudex.core.VisitURL;
import iudex.filter.Filter;

import org.slf4j.MDC;

import com.gravitext.htmap.UniMap;

public class UHashMDCSetter implements Filter
{
    @Override
    public boolean filter( UniMap content )
    {
        VisitURL url = content.get( URL );
        MDC.put( "uhash", ( url != null ) ? url.uhash() : "¡no-visit-URL!"  );

        return true;
    }
}
