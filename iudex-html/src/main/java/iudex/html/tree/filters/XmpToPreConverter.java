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

package iudex.html.tree.filters;

import static iudex.html.HTMLTag.htmlTag;
import iudex.html.HTML;
import iudex.html.tree.TreeFilter;

import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.Node;

/**
 * Convert &lt;xmp> to &lt;pre>. XMP is deprecated in later HTML versions and is
 * X(HT)ML incompatible, but can still can be found in the wild. After the HTML
 * parse where special internal markup rules are applied, it is effectively the
 * same as PRE, and will cause less trouble if converted to PRE.This should be
 * applied before any filter with specific behavior for PRE.
 */
public final class XmpToPreConverter implements TreeFilter
{
    @Override
    public Action filter( Node node )
    {
        final Element elem = node.asElement();
        if( ( elem != null ) && htmlTag( elem ) == HTML.XMP ) {
            elem.setTag( HTML.PRE );
        }

        return Action.CONTINUE;
    }
}
