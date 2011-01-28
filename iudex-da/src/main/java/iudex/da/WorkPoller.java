/*
 * Copyright (c) 2008-2010 David Kellum
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

package iudex.da;

import iudex.core.GenericWorkPollStrategy;
import iudex.core.VisitQueue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import static iudex.core.ContentKeys.*;
import static iudex.da.ContentMapper.HOST;

public class WorkPoller extends GenericWorkPollStrategy
{
    public WorkPoller( DataSource dataSource, ContentMapper mapper )
    {
        for( Key req : REQUIRED_KEYS ) {
            if( ! mapper.fields().contains( req ) ) {
                throw new IllegalArgumentException(
                   "WorkPoller needs mapper with " + req + " included." );
            }
        }

        _contentReader = new ContentReader( dataSource, mapper );
    }

    public void setHostDepthDivisor( float hostDepthDivisor )
    {
        _hostDepthDivisor = hostDepthDivisor;
    }

    public float hostDepthDivisor()
    {
        return _hostDepthDivisor;
    }

    public void setMaxPriorityUrls( int maxPriorityUrls )
    {
        _maxPriorityUrls = maxPriorityUrls;
    }

    public int maxPriorityUrls()
    {
        return _maxPriorityUrls;
    }

    public void setMaxHostUrls( int maxHostUrls )
    {
        _maxHostUrls = maxHostUrls;
    }

    public int maxHostUrls()
    {
        return _maxHostUrls;
    }

    /**
     * Set maximum VisitURLs to load per poll.
     */
    public void setMaxUrls( int maxUrls )
    {
        _maxUrls = maxUrls;
    }

    public int maxUrls()
    {
        return _maxUrls;
    }

    public List<UniMap> poll() throws SQLException
    {
        CharSequence fs = _contentReader.mapper().fieldNames();
        CharSequence fsh = fs;
        if( ! _contentReader.mapper().fields().contains( HOST ) ) {
            fsh = fs.toString() + ", host";
        }

        String query = String.format( POLL_QUERY, fs, fsh, fsh, fsh, fsh );

        return _contentReader.select( query,
                                      _hostDepthDivisor,
                                      _maxPriorityUrls,
                                      _maxHostUrls,
                                      _maxUrls );
    }

    @Override
    public void pollWorkImpl( VisitQueue out )
    {
        try {
            //FIXME: Further optimization potential by utilizing grouping
            //of URLs for common host.
            out.addAll( poll() );
        }
        catch( SQLException x ) {
            //FIXME: Or log it and continue? Allow # of failures before passing?
            throw new RuntimeException( x );
        }
    }

    private static final String POLL_QUERY =
    "SELECT %s " +
    "FROM ( SELECT %s " +
    "       FROM ( SELECT %s, ( priority - ( ( hpos - 1 ) / ? ) ) AS adj_priority " +
    "              FROM ( SELECT %s, " +
    "                            row_number() OVER ( PARTITION BY host " +
    "                                                ORDER BY priority DESC ) AS hpos " +
    "                     FROM ( SELECT %s " +
    "                            FROM urls " +
    "                            WHERE next_visit_after <= now() " +
    "                            ORDER BY priority DESC " +
    "                            LIMIT ? " +
    "                          ) AS subP " +
    "                   ) AS subH " +
    "              WHERE hpos <= ? " +
    "            ) AS subA " +
    "      ORDER BY adj_priority DESC " +
    "      LIMIT ? ) AS subF " +
    "ORDER BY host, priority DESC; ";

    private static final List<Key> REQUIRED_KEYS =
        Arrays.asList( new Key[] { URL, PRIORITY, NEXT_VISIT_AFTER } );

    private final ContentReader _contentReader;
    private float _hostDepthDivisor = 8.0f;

    private int _maxPriorityUrls = 500000;
    private int _maxHostUrls     =  10000;
    private int _maxUrls         =  50000;
}
