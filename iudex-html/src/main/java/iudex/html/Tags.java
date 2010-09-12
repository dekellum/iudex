/*
 * Copyright (c) 2010 David Kellum
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

package iudex.html;

import com.gravitext.xml.NamespaceCache;

import com.gravitext.xml.producer.Namespace;
import com.gravitext.xml.producer.Tag;
import com.gravitext.xml.producer.Attribute;

public class Tags
{
    private NamespaceCache _cache = new NamespaceCache();


    private Namespace NS_XHTML =
        _cache.namespace( null, "http://www.w3.org/1999/xhtml" );
}
