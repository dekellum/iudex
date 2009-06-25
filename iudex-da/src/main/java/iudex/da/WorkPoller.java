/*
 * Copyright (C) 2008-2009 David Kellum
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

        return select( query, _urlsPerHost, _totalUrls );
    }

    private static final String POLL_QUERY =
        "SELECT %s " +
        "FROM ( SELECT * FROM urls " +
        "       WHERE next_visit_after <= now() " +
        "       AND uhash IN ( SELECT uhash FROM urls o " +
        "                      WHERE o.host = urls.host " +
        "                      ORDER BY priority DESC LIMIT ? ) " +
        "       ORDER BY priority DESC LIMIT ? ) AS sub " +
        "ORDER BY host, priority DESC;";

    private int _urlsPerHost = 100;
    private int _totalUrls = 10000;
}
