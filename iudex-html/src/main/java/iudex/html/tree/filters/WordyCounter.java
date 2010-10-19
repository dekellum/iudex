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

import static iudex.html.HTMLTag.*;
import static iudex.html.tree.HTMLTreeKeys.*;

import iudex.html.HTML;
import iudex.html.HTMLTag;
import iudex.html.tree.TreeFilter;

import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.Node;

public class WordyCounter implements TreeFilter
{
    //FIXME: Define method to insure this is only run depth first?

    @Override
    public Action filter( Node node )
    {
        Element elem = node.asElement();
        if( elem != null ) {
            float inlineSum = 0.0f;
            float blockSum  = 0.0f;

            for( Node child : elem.children() ) {
                final Integer cwords = child.get( WORD_COUNT );
                if( cwords != null ) {
                    Element celm = child.asElement();
                    if( celm != null ) {
                        final Float cwordy = celm.get( WORDINESS );
                        if( isInline( celm ) ) {
                            inlineSum += cwordy;
                        }
                        else {
                            blockSum += ( cwordy * cwords );
                        }
                    }
                    else {
                        inlineSum += cwords;
                    }
                }
            }

            float sum = inlineSum;

            int ewc = elem.get( WORD_COUNT );
            if( ewc > 0 ) sum += ( blockSum / ewc );

            //FIXME: Really just additive in inline plus block case?

            sum = adjustByTag( elem, sum );

            elem.set( WORDINESS, sum );
        }

        return Action.CONTINUE;
    }

    private float adjustByTag( Element elem, float sum )
    {
        final HTMLTag tag = htmlTag( elem );

        //FIXME: Just examples.
        if( tag == HTML.A ) {
            CharSequence href = elem.attribute( HTML.ATTR_HREF );
            if( ( href != null ) && href.length() > 0 ) {
                sum *= 0.25f;
            }
        }
        else if( tag == HTML.H1 ) {
            sum *= 2.0f;
        }
        else if( tag == HTML.H2 ) {
            sum *= 1.8f;
        }
        else if( tag == HTML.H3 ) {
            sum *= 1.6f;
        }

        return sum;
    }
}
