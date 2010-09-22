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

package iudex.html.tree;

import iudex.html.tree.TreeFilter.Action;
import static iudex.html.tree.TreeFilter.Action.*;

import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.Node;

public class TreeWalker
{
    public static Action walkDepthFirst( TreeFilter filter,  Node node )
    {
        Action action = CONTINUE;

        Element element = node.asElement();
        if( element != null ) {
            for( Node child : element.children() ) {
                action = walkDepthFirst( filter, child );
                switch( action ) {
                case DROP:
                    child.detach();
                    break;
                case TERMINATE:
                    return TERMINATE;
                }
            }
        }

        return filter.filter( node );
    }

    public static Action walkBreadthFirst( TreeFilter filter,  Node node )
    {
        Action action = filter.filter( node );
        if( action == CONTINUE || action == CHAIN_END ) {
            Element element = node.asElement();
            if( element != null ) {
                loop: for( Node child : element.children() ) {
                    Action childAction = walkBreadthFirst( filter, child );
                    switch( childAction ) {
                    case DROP:
                        child.detach();
                        break;
                    case TERMINATE:
                        break loop;
                    }
                }
            }
        }
        return action;
    }
}
