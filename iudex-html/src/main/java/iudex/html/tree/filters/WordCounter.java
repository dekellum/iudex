/*
 * Copyright (c) 2010-2015 David Kellum
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
import static iudex.util.Characters.*;

import static iudex.html.tree.HTMLTreeKeys.*;

import iudex.html.tree.TreeFilter;

public final class WordCounter implements TreeFilter
{
    //FIXME: Define method to insure this is only run depth first?

    @Override
    public Action filter( Node node )
    {
        int wcount = 0;

        final Element elem = node.asElement();
        if( elem != null ) {
            for( Node child : elem.children() ) {
                Integer count = child.get( WORD_COUNT );
                if( count != null ) wcount += count;
            }
        }
        else {
            wcount = wordCount( node.characters() );
        }

        node.set( WORD_COUNT, wcount );

        return Action.CONTINUE;
    }
}
