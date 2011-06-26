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

package iudex.util;

import java.nio.charset.Charset;

public class Charsets extends com.gravitext.util.Charsets
{
    public static final Charset WINDOWS_1252 =
        Charset.forName( "windows-1252" );

    /**
     * Expand provided encoding to a super-set encoding if possible, to maximize
     * character mappings.
     * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/parsing.html#character-encodings-0
     */
    public static Charset expand( final Charset in ) {
        Charset out = in;

        if( in.equals( ASCII ) ||
            in.equals( ISO_8859_1 ) ) out = WINDOWS_1252;

        //FIXME: Add expansion mappings for non-english languages.

        return out;
    }

    public static Charset defaultCharset()
    {
        return _default;
    }

    private static Charset _default = WINDOWS_1252;
}
