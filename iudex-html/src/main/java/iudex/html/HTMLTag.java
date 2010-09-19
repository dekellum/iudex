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

package iudex.html;

import com.gravitext.xml.producer.Namespace;
import com.gravitext.xml.producer.Tag;

public final class HTMLTag extends Tag
{
    public static enum Flag {
        DEPRECATED,
        INLINE,
        METADATA,
        BANNED
    }

    public HTMLTag( String name, Namespace ns, Flag...flags )
    {
        super( name, ns );

        _deprecated = find( flags, Flag.DEPRECATED );
        _inline     = find( flags, Flag.INLINE );
        _metadata   = find( flags, Flag.METADATA );
        _banned     = find( flags, Flag.BANNED );
    }

    public boolean isDeprecated()
    {
        return _deprecated;
    }

    public boolean isInline()
    {
        return _inline;
    }

    public boolean isMetadata()
    {
        return _metadata;
    }

    public boolean isBanned()
    {
        return _banned;
    }

    private static boolean find( Flag[] flags, Flag flag )
    {
        for( Flag f : flags ) {
            if( f == flag ) return true;
        }
        return false;
    }

    private final boolean _deprecated;
    private final boolean _inline;
    private final boolean _metadata;
    private final boolean _banned;
}
