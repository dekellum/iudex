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
import java.util.List;

import javax.sql.DataSource;

import com.gravitext.htmap.UniMap;

public class WorkPoller extends GenericWorkPollStrategy
{
    public WorkPoller( DataSource dataSource, ContentMapper mapper )
    {
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
        String query = String.format( POLL_QUERY,
                                      _contentReader.mapper().fieldNames() );

        return _contentReader.select( query, _hostDepthDivisor, _maxUrls );
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
    "FROM ( SELECT *, " +
    "       row_number() OVER( ORDER BY adj_priority DESC ) as apos " +
    "       FROM ( SELECT *, " +
    "              ( priority -  " +
    "               ( ( row_number() OVER " +
    "                   ( PARTITION BY host ORDER BY priority DESC ) - 1 ) " +
    "                  / ? ) ) as adj_priority " +
    "              FROM urls " +
    "              WHERE next_visit_after <= now() ) AS sub1 ) as sub2 " +
    "WHERE apos <= ? " +
    "ORDER BY host, priority DESC; ";

    private final ContentReader _contentReader;
    private float _hostDepthDivisor = 8.0f;
    private int _maxUrls = 50000;
}
