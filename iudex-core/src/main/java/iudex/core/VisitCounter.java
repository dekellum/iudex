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

package iudex.core;

import com.gravitext.htmap.UniMap;

public interface VisitCounter
{
    /**
     * Add a new order.
     */
    void add( UniMap order );

    /**
     * Release reference on host/domain previously obtained and possibly
     * add a new order.
     * @param acquired order with unchanged ContentKeys.URL from acquire.
     * @param newOrder optional, possibly new order to add with this release.
     */
    void release( UniMap acquired, UniMap newOrder );

}
