/*
 * Copyright (c) 2008-2012 David Kellum
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

import com.gravitext.htmap.Key;
import com.gravitext.xml.tree.Node;

public class HTMLTreeKeys
{
    /**
     * The raw unadjusted sum of words counted at the attached Node and
     * its descendants. Thus the WORD_COUNT of the root node is the total for
     * the entire document.
     */
    public static final Key<Integer> WORD_COUNT =
        Node.KEY_SPACE.create( "word_count", Integer.class );

    /**
     * A heuristic representing the <i>wordiness<i> of an element and its
     * descendants.
     */
    public static final Key<Float> WORDINESS =
        Node.KEY_SPACE.create( "wordiness", Float.class );

}
