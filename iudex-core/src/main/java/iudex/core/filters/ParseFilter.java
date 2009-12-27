package iudex.core.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.gravitext.htmap.UniMap;

import iudex.core.ContentKeys;
import iudex.core.ContentParser;
import iudex.core.ParseException;
import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.filter.FilterException;

public class ParseFilter implements Filter, Described
{
    public ParseFilter( ContentParser parser )
    {
        _parser = parser;
    }

    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        try {
             Iterator<UniMap> iter = _parser.parse( content );

             ArrayList<UniMap> refs = new ArrayList<UniMap>();
             while( iter.hasNext() ) {
                 refs.add( iter.next() );
             }
             content.set( ContentKeys.REFERENCES, refs );
             return true;
        }
        catch( ParseException e ) {
           throw new FilterException( e );
        }
        catch( IOException e ) {
            throw new FilterException( e );
        }
    }

    @Override
    public List<?> describe()
    {
        return Arrays.asList( _parser.getClass().getSimpleName() );
    }

    private ContentParser _parser;
}
