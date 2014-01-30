/*
 * Copyright (c) 2010-2014 David Kellum
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

import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.Node;

import static iudex.html.HTMLTag.htmlTag;

import iudex.html.tree.TreeFilter;

/**
 * SKIP all elements of tag.isMetadata(). This includes HTML.HEAD and all of its
 * legal children, HTML.TITLE, etc.
 */
public final class MetaSkipFilter implements TreeFilter
{
    @Override
    public Action filter( Node node )
    {
        final Element elem = node.asElement();
        return ( ( ( elem != null ) && htmlTag( elem ).isMetadata() ) ?
                 Action.SKIP : Action.CONTINUE );
    }
}
