/*
 * Copyright (c) 2008-2013 David Kellum
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

package iudex.da;

import java.util.Date;

import com.gravitext.htmap.Key;

import iudex.core.ContentKeys;

public class DAKeys extends ContentKeys
{
    /**
     * A unique identifier for the iudex worker instance that has reserved or
     * last processed an order.
     */
    public static final Key<String> INSTANCE =
        create( "instance", String.class );

    /**
     * The date at which time an order was reserved.
     */
    public static final Key<Date> RESERVED =
        create( "reserved", Date.class );

    /**
     * If set true, indicates that content came from WorkPoller with
     * PRIORITY adjusted from the actual value.
     */
    public static final Key<Boolean> PRIORITY_ADJUSTED =
        create( "priority_adjusted", Boolean.class );
}
