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

import java.util.ArrayList;
import java.util.List;

import com.gravitext.xml.producer.Attribute;
import com.gravitext.xml.tree.AttributeValue;
import com.gravitext.xml.tree.Element;
import com.gravitext.xml.tree.Node;

import iudex.html.HTMLTag;
import iudex.html.tree.TreeFilter;

/**
 * Drops attributes outside the "basic" (non-style) set for each element.
 * Compatible with both BREADTH_FIRST and DEPTH_FIRST traversal.
 */
public final class AttributeCleaner implements TreeFilter
{
    @Override
    public Action filter( Node node )
    {
        final Element elem = node.asElement();
        if( elem != null ) {
            final List<Attribute> accept =
                HTMLTag.htmlTag( elem ).basicAttributes();
            final List<AttributeValue> attsIn = elem.attributes();

            List<AttributeValue> attsOut = null;
            final int end = attsIn.size();
            for( int i = 0; i < end; ++i ) {
                final AttributeValue att = attsIn.get( i ) ;
                if( ! accept.contains( att.attribute() ) ) {
                    if( attsOut == null ) {
                        attsOut = copyTo( attsIn, i );
                    }
                }
                else if( attsOut != null ) {
                    attsOut.add( att );
                }
            }

            if( attsOut != null ) elem.setAttributes( attsOut );
        }

        return Action.CONTINUE;
    }

    private List<AttributeValue> copyTo( final List<AttributeValue> attsIn,
                                         final int end )
    {
        final List<AttributeValue> attsOut =
            new ArrayList<AttributeValue>( attsIn.size() - 1 );

        for( int i = 0; i < end; ++i ) {
            attsOut.add( attsIn.get( i ) );
        }

        return attsOut;
    }
}
