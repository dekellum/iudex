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


import iudex.core.VisitURL;
import iudex.core.VisitURL.SyntaxException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.UniMap;

import static org.junit.Assert.*;
import static iudex.core.ContentKeys.*;

public class ContentUpdaterTest
{
    private static DataSource _dataSource = null;

    @BeforeClass
    public static void Setup() throws SQLException
    {      
        _dataSource = DataSourceFactory.create();
        QueryRunner runner = new QueryRunner( _dataSource );
        runner.update( "DELETE from urls;" );
    }
    
    @AfterClass
    public static void Close()
    {
        //FIXME: Delete?
    }
    
   
    @Test
    public void test() throws SQLException, SyntaxException
    {
        ContentUpdater updater = new ContentUpdater( _dataSource );
        List<UniMap> contents = new ArrayList<UniMap>();
        UniMap c = new UniMap();
        c.set( URL, 
               VisitURL.normalize( "http://gravitext.com/blog/feed/atom.xml" ) );
        c.set( TYPE, TYPE_FEED );
        c.set( PRIORITY, 1f );
        c.set( NEXT_VISIT_AFTER, new Date() );
        //FIXME: Shouldn't need to set, express default?
        contents.add( c );
        try {
            assertEquals( 1, updater.write( contents ) );
            
            c = new UniMap();
            c.set( URL, 
                   VisitURL.normalize( "http://gravitext.com/content" ) );
            c.set( TYPE, TYPE_PAGE );
            c.set( PRIORITY, 1f );
            c.set( NEXT_VISIT_AFTER, new Date() );
            contents.add( c );
            updater.update( contents );
        }
        catch( SQLException x ) {
            //FIXME: Move to ContentWriter?
            _log.error( x.getMessage() );
            SQLException n = x;
            while( (n = n.getNextException() ) != null ) {
                _log.error(  n.getMessage() );
            }
            throw x;
        }
        
        WorkPoller wpoller = new WorkPoller( _dataSource );
        contents = wpoller.poll();
        _log.info( "Work poll returned {} contents", contents.size() );
        
        for( UniMap w : contents ) {
            _log.info( w.toString() );
        }
        assertNotNull( contents );
    }
    public final Logger _log = LoggerFactory.getLogger( getClass() );
    
}
