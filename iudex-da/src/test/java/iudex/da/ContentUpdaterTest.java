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

import iudex.core.VisitURL.SyntaxException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.UniMap;

import static org.junit.Assert.*;
import static iudex.core.ContentKeys.*;
import static iudex.da.ContentMapper.*;

public class ContentUpdaterTest
    extends TestHelper
{
    @Test
    public void test() throws SQLException, SyntaxException
    {
        ContentMapper kmap =
            new ContentMapper( UHASH, HOST, URL, TYPE, REF_PUB_DATE, 
                               PRIORITY, NEXT_VISIT_AFTER );
                                                  
        ContentUpdater updater = new ContentUpdater( dataSource(), kmap );
        List<UniMap> in = new ArrayList<UniMap>();
        in.add( content( "http://gravitext.com/blog/feed/atom.xml", 
                               TYPE_FEED, 1f ) );
        
        assertEquals( 1, updater.write( in ) );
        
        in.add( content( "http://gravitext.com/content", TYPE_PAGE, 1f ));
        
        updater.update( in );

        ContentReader reader = new ContentReader( dataSource(), kmap );
        List<UniMap> out = reader.select( "SELECT " + kmap.fieldNames() + 
                           " FROM urls ORDER BY priority desc"  );

        assertEquals( in, out );
    }
    public final Logger _log = LoggerFactory.getLogger( getClass() );
    
}
