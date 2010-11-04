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

import java.util.List;

import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.Node;

import static iudex.util.Characters.*;

import iudex.html.HTML;
import iudex.html.HTMLTag;
import iudex.html.tree.TreeFilter;
import iudex.util.Characters;

/**
 * Normalizes character nodes by replacing control characters and minimizing
 * whitespace. Is aware of whitespace significance rules in HTML
 * &lt;pre> blocks as well as block vs inline elements in general. Compatible
 * with both BREADTH_FIRST and DEPTH_FIRST traversal.
 */
public class CharactersNormalizer implements TreeFilter
{
    @Override
    public Action filter( Node node )
    {
        final Element elem = node.asElement();
        if( elem != null ) {
            final boolean parentIsBlock = isBlock( elem );
            final List<Node> children = elem.children();
            final boolean inPre = inPreBlock( elem );

            int i = 0;
            while( i < children.size() ) {
                Node cc = children.get( i );
                if( cc.isCharacters() ) {

                    final CharSequence clean = inPre ?
                        cleanPreBlock( cc.characters() ) :
                        clean( parentIsBlock, children, i );

                    if( clean.length() > 0 ) {
                        cc.setCharacters( clean );
                        ++i;
                    }
                    else cc.detach();
                }
                else ++i;
            }
        }

        return Action.CONTINUE;
    }

    private CharSequence clean( final boolean parentIsBlock,
                                final List<Node> children,
                                final int i )
    {
        final boolean trimL = parentIsBlock &&
            ( ( i == 0 ) || isBlock( children.get( i-1 ) ) );
        final boolean trimR = parentIsBlock &&
            ( ( i == (children.size()-1) ) || isBlock( children.get( i+1 ) ) );
        final boolean both = trimL && trimR;

        CharSequence clean = replaceCtrlWS( children.get( i ).characters(),
                                            " ", both );
        int clen = clean.length();

        if( !both && clen > 0 ) {
            if( trimL && ( clean.charAt( 0 ) == ' ' ) ) {
                clean = clean.subSequence( 1, clen );
            }
            else if( trimR && ( clean.charAt( clen-1 ) == ' ' ) ) {
                clean = clean.subSequence( 0, clen-1 );
            }
        }
        return clean;
    }

    private boolean isBlock( Node node )
    {
        Element elem = node.asElement();
        return ( ( elem != null ) && ! HTMLTag.isInline( elem ) );
    }

    private boolean inPreBlock( Element elem )
    {
        while( elem != null ) {
            HTMLTag tag = HTMLTag.htmlTag( elem );
            if( tag == HTML.PRE ) return true;
            else if( ! tag.isInline() ) break;
            elem = elem.parent();
        }
        return false;
    }

    private CharSequence cleanPreBlock( CharSequence chars )
    {
        return Characters.replaceCtrl( chars, ' ' );
    }
}
