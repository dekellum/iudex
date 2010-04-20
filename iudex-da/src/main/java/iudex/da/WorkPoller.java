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

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import com.gravitext.htmap.UniMap;

public class WorkPoller
    extends ContentReader
{
    public WorkPoller( DataSource dataSource, ContentMapper mapper )
    {
        super( dataSource, mapper );
    }

    public List<UniMap> poll() throws SQLException
    {
        String query = String.format( POLL_QUERY, mapper().fieldNames() );

        return select( query, _totalUrls );
    }

    private static final String POLL_QUERY =
    "SELECT %s " +
    "FROM ( SELECT *, " +
    "       row_number() OVER( ORDER BY adj_priority DESC ) as apos " +
    "       FROM ( SELECT *, " +
    "              ( priority -  " +
    "               ( ( row_number() OVER " +
    "                   ( PARTITION BY host ORDER BY priority DESC ) - 1 ) " +
    "                  / 8.0 ) ) as adj_priority " +
    "              FROM urls " +
    "              WHERE next_visit_after <= now() ) AS sub1 ) as sub2 " +
    "WHERE apos <= ? " +
    "ORDER BY host, priority DESC; ";

    private int _totalUrls = 50000;
}
