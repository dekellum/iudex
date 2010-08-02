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

package iudex.filter.core;

import java.util.List;

import iudex.filter.Filter;
import iudex.filter.FilterException;
import iudex.filter.FilterListener;

import com.gravitext.htmap.UniMap;
import com.gravitext.util.Closeable;
import com.gravitext.util.Closeables;

public class ListenerChain implements FilterListener, Closeable
{
    public ListenerChain( List<FilterListener> listeners )
    {
        _listeners = listeners.toArray( new FilterListener[listeners.size()] );
    }

    @Override
    public void accepted( UniMap result )
    {
        for( FilterListener listener : _listeners ) {
           listener.accepted( result );
        }
    }

    @Override
    public void rejected( Filter filter, UniMap reject )
    {
        for( FilterListener listener : _listeners ) {
            listener.rejected( filter, reject );
         }
    }

    @Override
    public void failed( Filter filter, UniMap reject, FilterException x )
    {
        for( FilterListener listener : _listeners ) {
            listener.failed( filter, reject, x );
         }
    }

    @Override
    public void close()
    {
        for( FilterListener listener : _listeners ) {
            Closeables.closeIf( listener );
        }
    }

    public final FilterListener[] _listeners;
}
