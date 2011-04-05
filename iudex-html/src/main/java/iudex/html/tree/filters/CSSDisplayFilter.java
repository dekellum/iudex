/*
 * Copyright (c) 2010-2011 David Kellum
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

import java.util.regex.Pattern;

import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.Node;

import iudex.html.HTML;
import iudex.html.tree.TreeFilter;

/**
 * Drop elements with attribute style="display:none". Note this should be
 * applied before {@link AttributeCleaner} which removes style attributes.
 */
public final class CSSDisplayFilter implements TreeFilter
{
    @Override
    public Action filter( Node node )
    {
        Action action = Action.CONTINUE;

        final Element elem = node.asElement();
        if( elem != null ) {
            CharSequence style = elem.attribute( HTML.ATTR_STYLE );
            if( ( style != null ) && hasDisplayNone( style ) ) {
                action = Action.DROP;
            }
        }

        return action;
    }

    boolean hasDisplayNone( CharSequence style )
    {
        return DISPLAY_NONE_RE.matcher( style ).find();
    }

    private static final Pattern DISPLAY_NONE_RE =
        Pattern.compile( "(^|[\\s;{])display\\s*:\\s*none([\\s;}]|$)",
                         Pattern.CASE_INSENSITIVE );
}
