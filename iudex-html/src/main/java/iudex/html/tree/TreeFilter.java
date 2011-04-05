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

package iudex.html.tree;

import com.gravitext.xml.tree.Node;

public interface TreeFilter
{
   public enum Action
   {
       /**
        * Continue executing filter chain for this element.
        */
       CONTINUE,

       /**
        * Stop execution of filter chain for this element.
        */
       CHAIN_END,

       /**
        * Don't descend into this element. Only applies to breadth first
        * descent.
        */
       SKIP,

       /**
        * Replace this element with its children. Equivalent to DROP if return
        * for a non-Element node, or an element with no children.
        */
       FOLD,

       /**
        * Detach this element, and its children, from the tree. In breadth
        * first descent, children will never be processed.
        */
       DROP,

       /**
        * Terminate the tree walk.
        */
       TERMINATE
   }

   Action filter( Node node );
}
