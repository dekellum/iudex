/*
 * Copyright (c) 2011 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package iudex.core.filters;

import static iudex.core.ContentKeys.*;
import static iudex.http.HTTPKeys.*;

import java.util.ArrayList;
import java.util.List;

import com.gravitext.htmap.UniMap;

import iudex.core.VisitCounter;
import iudex.core.VisitURL;
import iudex.core.VisitURL.SyntaxException;
import iudex.filter.Filter;
import iudex.filter.FilterException;
import iudex.http.HTTPSession;
import iudex.http.Header;
import iudex.http.Headers;

public class RedirectHandler implements Filter
{
    public RedirectHandler( VisitCounter releaser )
    {
        _releaser = releaser;
    }

    public void setMaxPath( int maxPath )
    {
        _maxPath = maxPath;
    }

    public int maxPath()
    {
        return _maxPath;
    }

    @Override
    public boolean filter( UniMap order ) throws FilterException
    {
        int status = order.get( STATUS );
        if( ( status == 301 ) ||
            ( status == 302 ) ||
            ( status == 303 ) ||
            ( status == 307 ) ) {

            //FIXME: Add 300 as well?

            List<Header> headers = order.get( RESPONSE_HEADERS );
            Header locHeader = Headers.getFirst( headers, "Location" );
            if( locHeader != null ) {
                final VisitURL oldUrl = order.get( URL );
                try {
                    VisitURL newUrl = oldUrl.resolve(
                        Headers.asCharSequence( locHeader.value() ) );

                    List<VisitURL> path = order.get( REDIRECT_PATH );
                    if( path == null ) {
                        path = new ArrayList<VisitURL>( 3 );
                    }

                    if( oldUrl.equals( newUrl ) ||
                        path.contains( newUrl ) ||
                        path.size() >= _maxPath  ) {

                        //FIXME: Log these as debug at minimum?
                        return true;
                    }

                    path.add( oldUrl );
                    order.set( REDIRECT_PATH, path );

                    // FIXME: Better to explicitly copy only certain known keys
                    // from old order to new. For example, don't keep
                    // HTTP statuses, header in new?

                    UniMap referer = order.get( REFERER );
                    if( referer == null ) {
                        referer = order.clone();
                        order.set( REFERER, referer );
                    }

                    // FIXME: Avoid circular reference of referer to referent,
                    // and stack overflow on toString, by just making a copy
                    // with URL.
                    UniMap referent = new UniMap();
                    referent.set( URL, newUrl );
                    referer.set( REFERENT, referent );

                    // FIXME: Change release interface to just take this url?
                    UniMap old = new UniMap();
                    old.set( URL, oldUrl );

                    order.set( URL, newUrl );

                    //FIXME: Set old as LAST key?

                    //Release here, add new work new filter?

                    _releaser.release( old, order );

                    // FIXME: Split this up into two filters. Possible moving
                    // part one up into the ContentFilter itself, allowing
                    // additional logic to decide to follow the newUrl or not,
                    // database read for newUrl, etc.

                    // The newUrl order will be processed in due course, so
                    // this chain should end.
                    return false;
                }
                catch( SyntaxException e ) {
                    order.set( STATUS, HTTPSession.INVALID_REDIRECT_URL );
                    order.set( REASON, "i.c.f.RedirectHandler: " + e );
                }
            }
            //FIXME: Log no Location header case.
        }

        return true;
    }

    private final VisitCounter _releaser;
    private int _maxPath = 6;
}
