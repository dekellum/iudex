/*
 * Copyright (c) 2008-2012 David Kellum
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

import iudex.core.ContentKeys;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

public class BaseTransformer implements Transformer
{
    /**
     * {@inheritDoc}
     * This implementation uses {@link #merge()}.
     */
    public UniMap transformContent( UniMap updated, UniMap current )
    {
        return merge( updated, current );
    }

    /**
     * {@inheritDoc}
     * This implementation uses {@link #merge()}.
     */
    public UniMap transformReferer( UniMap updated, UniMap current )
    {
        return merge( updated, current );
    }

    /**
     * {@inheritDoc}
     * This implementation uses {@link #merge()}.
     */
    public UniMap transformReference( UniMap updated, UniMap current )
    {
        return merge( updated, current );
    }

    /**
     * If current is null, returns updated, else returns a clone of current with
     * updated merged.
     */
    protected UniMap merge( final UniMap updated, final UniMap current )
    {
        if( current != null ) augment( updated, current );

        // FIXME: Use updated.augment( current ) when available.

        return updated;
    }

    @SuppressWarnings("unchecked")
    private final void augment( UniMap updated, UniMap current )
    {
        Boolean adjustedObj = updated.get( DAKeys.PRIORITY_ADJUSTED );
        final boolean adjusted = ( adjustedObj != null ) && adjustedObj;

        // Special case for priority: Since WorkPoller may adjust the
        // priority, for example by age, prefer the current priority
        // from the database if indicated. ContentUpdater filters
        // still get a chance to update from the normal value.

        for( Key key : UniMap.KEY_SPACE.keys() ) {
           Object value = current.get( key );
           if( ( value != null ) &&
               ( ( ( key == ContentKeys.PRIORITY ) && adjusted ) ||
                 !updated.containsKey( key ) ) ) {
               updated.set( key, value );
           }
        }
    }
}
