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

package iudex.html;

import iudex.core.ContentKeys;

import com.gravitext.htmap.Key;
import com.gravitext.xml.tree.Element;

public class HTMLKeys extends ContentKeys
{
    public static final Key<Element> TITLE_TREE =
        create( "title_tree", Element.class );

    public static final Key<Element> SUMMARY_TREE =
        create( "summary_tree", Element.class );

    public static final Key<Element> CONTENT_TREE =
        create( "content_tree", Element.class );

    public static final Key<Element> SOURCE_TREE =
        create( "source_tree", Element.class );
}
