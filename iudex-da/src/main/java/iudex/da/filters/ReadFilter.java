/*
 * Copyright (c) 2008-2015 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package iudex.da.filters;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import iudex.da.ContentMapper;
import iudex.da.ContentReader;
import iudex.filter.Filter;

import static iudex.da.ContentMapper.*;

/**
 * Filter based support for updating references (from feeds, etc.) and the
 * original content as part of a single batch transaction.
 */
public class ReadFilter
    extends ContentReader
    implements Filter
{
    public ReadFilter( DataSource source, ContentMapper mapper )
    {
        super( source, mapper );
        for( Key req : REQUIRED_KEYS ) {
            if( ! mapper.fields().contains( req ) ) {
                throw new IllegalArgumentException(
                   "UpdateFilter needs mapper with " + req + " included." );
            }
        }
    }

    @Override
    public boolean filter( UniMap content )
    {
        try {
            read( content );
            return true;
        }
        catch( SQLException x ) {
            SQLException s = x;
            while( s != null ) {
                _log.error( "On Read: ({}) {}", s.getSQLState(), s.toString() );
                s = s.getNextException();
            }
            return false;
        }
    }

    private static final List<Key> REQUIRED_KEYS =
        Arrays.asList( new Key[] { UHASH } );

    protected static final Logger _log =
        LoggerFactory.getLogger( ReadFilter.class );
}
