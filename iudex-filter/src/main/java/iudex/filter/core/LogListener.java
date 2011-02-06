/*
 * Copyright (c) 2008-2011 David Kellum
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

package iudex.filter.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iudex.filter.Filter;
import iudex.filter.FilterException;
import iudex.filter.FilterListener;

import com.gravitext.htmap.UniMap;

public class LogListener implements FilterListener
{
    public LogListener( String logBase,
                        FilterIndex index )
    {
        _logAccept = LoggerFactory.getLogger( logBase + ".accept" );
        _logReject = LoggerFactory.getLogger( logBase + ".reject" );
        _logFailed = LoggerFactory.getLogger( logBase + ".failed" );

        _index = index;
    }

    @Override
    public void accepted( final UniMap result )
    {
        _logAccept.debug( "" );
    }

    @Override
    public void rejected( final Filter filter, final UniMap reject )
    {
        _logReject.debug( "by {}", name( filter ) );
    }

    @Override
    public void failed( final Filter filter,
                        final UniMap reject,
                        final FilterException x )
    {
        _logFailed.warn( "from {} : {}", name( filter ), x  );
        _logFailed.debug( "Stack: ", x );
    }

    private String name( final Filter filter )
    {
        return _index.name( filter );
    }

    private final Logger _logAccept;
    private final Logger _logReject;
    private final Logger _logFailed;
    private final FilterIndex _index;
}
