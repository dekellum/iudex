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
import static iudex.http.HTTPSession.*;

import java.util.List;

import com.gravitext.htmap.UniMap;

import iudex.core.VisitCounter;
import iudex.core.VisitURL;
import iudex.core.VisitURL.SyntaxException;
import iudex.filter.Filter;
import iudex.http.Header;
import iudex.http.Headers;

public class RedirectHandler implements Filter
{
    public RedirectHandler( VisitCounter counter )
    {
        _visitCounter = counter;
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
    public boolean filter( UniMap order )
    {
        // Release the (old URL) order first, to minimize lease duration and
        // to insure it happens regardless.
        _visitCounter.release( order, null );

        try {
            if( isRedirect( order.get( STATUS ) ) ) {
                handle( order );
            }
        }
        catch( SyntaxException e ) {
            order.set( STATUS, INVALID_REDIRECT_URL );
            order.set( REASON, "i.c.f.RedirectHandler: " + e );
        }
        catch( BadRedirect e ) {
            order.set( STATUS, e.status() );
        }

        return true;
    }

    private void handle( UniMap order )
        throws SyntaxException, BadRedirect
    {
        final List<Header> headers = order.get( RESPONSE_HEADERS );
        Header locHeader = null;
        if( headers != null ) {
            locHeader = Headers.getFirst( headers, "Location" );
        }

        if( locHeader == null ) {
            throw new BadRedirect( MISSING_REDIRECT_LOCATION );
        }

        final VisitURL oldUrl = order.get( URL );
        final VisitURL newUrl =
            oldUrl.resolve( Headers.asCharSequence( locHeader.value() ) );

        checkPath( 1, order, newUrl );

        final UniMap revOrder = order.clone();
        revOrder.set( URL, newUrl );
        revOrder.set( PRIORITY, order.get( PRIORITY ) + _priorityIncrease );

        // HTTP state details don't apply to the new revOrder
        clean( revOrder );

        // Set REFERER if not already set
        UniMap referer = revOrder.get( REFERER );
        if( referer == null ) {
            referer = order;
            revOrder.set( REFERER, referer );
        }

        // Always set referer's referent to newest revOrder
        // (careful, this is circular)
        referer.set( REFERENT, revOrder );

        revOrder.set( LAST, order );

        // Set REVISIT_ORDER for {@link Revisitor}
        order.set( REVISIT_ORDER, revOrder );
    }

    private void checkPath( final int depth,
                            final UniMap order,
                            final VisitURL newUrl )
        throws BadRedirect
    {
        if( depth >= _maxPath ) {
            throw new BadRedirect( MAX_REDIRECTS_EXCEEDED );
        }
        if( newUrl.equals( order.get( URL ) ) ) {
            throw new BadRedirect( REDIRECT_LOOP );
        }
        final UniMap last = order.get( LAST );
        if( last != null ) checkPath( depth + 1, last, newUrl );
    }

    private void clean( UniMap revOrder )
    {
        revOrder.remove( STATUS );
        revOrder.remove( REASON );
        revOrder.remove( REQUEST_HEADERS );
        revOrder.remove( RESPONSE_HEADERS );
        revOrder.remove( ETAG );
    }

    private boolean isRedirect( int status )
    {
        //FIXME: Add 300 as well?

        return ( ( status == 301 ) ||
                 ( status == 302 ) ||
                 ( status == 303 ) ||
                 ( status == 307 ) );
    }

    private static class BadRedirect
        extends Exception
    {
        public BadRedirect( int status )
        {
            this( status, null );
        }

        public BadRedirect( int status, String message )
        {
            super( message );
            _status = status;
        }
        public int status()
        {
            return _status;
        }

        private final int _status;
    }

    private final VisitCounter _visitCounter;

    private int _maxPath = 6;
    private float _priorityIncrease = 0.5f;
}
