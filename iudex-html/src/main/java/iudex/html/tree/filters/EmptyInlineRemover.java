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

import java.util.List;

import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.Node;

import static iudex.html.HTMLTag.*;
import iudex.html.HTML;
import iudex.html.tree.TreeFilter;
import static iudex.util.Characters.*;

/**
 * Drop or fold no-op inline elements. Examples:
 *
 * <ul>
 *  <li> &lt;span/> :: DROP </li>
 *  <li> &lt;b>&lt;span/>&lt;/b> :: DROP </li>
 *  <li> &lt;b> &lt;br/> &lt;/b> :: FOLD </li>
 *  <li> &lt;img/> :: CONTINUE </li>
 * </ul>
 *
 * Should be run DEPTH_FIRST to allow recursive replacement.
 */
public final class EmptyInlineRemover implements TreeFilter
{
    @Override
    public Action filter( Node node )
    {
        Action action = Action.CONTINUE;

        final Element elem = node.asElement();
        if( ( elem != null ) && isInline( elem ) &&
            ( elem.tag() != HTML.IMG ) ) {

            final List<Node> children = elem.children();

            if( children.isEmpty() ) {
                action = Action.DROP;
            }
            else if( ( elem.tag() != HTML.PRE ) && allLogicalWS( children ) ) {
                action = Action.FOLD;
            }
        }
        return action;
    }

    private boolean allLogicalWS( List<Node> children )
    {
        for( Node c : children ) {
            if( c.isCharacters() ) {
                if( ! isEmptyCtrlWS( c.characters() ) ) return false;
            }
            else { //element
                if( c.asElement().tag() != HTML.BR ) return false;
            }
        }
        return true;
    }
}
