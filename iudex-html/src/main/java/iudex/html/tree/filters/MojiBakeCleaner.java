/*
 * Copyright (c) 2011 David Kellum
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

import java.util.Map;

import com.gravitext.xml.tree.Node;

import iudex.html.tree.TreeFilter;
import iudex.util.MojiBakeMapper;

/**
 * Attempts to recover Mojibake sequences in character nodes.
 * Compatible with both BREADTH_FIRST and DEPTH_FIRST traversal.
 */
public final class MojiBakeCleaner implements TreeFilter
{
    public MojiBakeCleaner( String regex,
                            Map<String,String> mojis )
    {
        _mapper = new MojiBakeMapper( regex, mojis );
    }

    @Override
    public Action filter( Node node )
    {
        if( node.isCharacters() ) {
            node.setCharacters( _mapper.recover( node.characters() ) );
        }

        return Action.CONTINUE;
    }

    private final MojiBakeMapper _mapper;
}
