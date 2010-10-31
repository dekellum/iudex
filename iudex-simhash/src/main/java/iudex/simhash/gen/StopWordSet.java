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

package iudex.simhash.gen;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.nio.CharBuffer;

public class StopWordSet
{
    public static final StopWordSet EMPTY_SET = new StopWordSet();

    public StopWordSet( Collection<String> words )
    {
        _tokens = new HashSet<CharBuffer>( words.size() * 3 / 2 );
        //FIXME: Or just use String tokens for faster lookup?

        int maxl = -1;

        for( String word : words ) {
            maxl = Math.max( maxl, word.length() );
            _tokens.add( CharBuffer.wrap( word ) );
        }
        _maxLength = maxl;
    }

    public boolean contains( CharBuffer token )
    {
        return ( ( token.remaining() <= _maxLength ) &&
                 _tokens.contains( token ) );
    }

    public Collection<CharBuffer> tokens()
    {
        return _tokens;
    }

    public int maxLength()
    {
        return _maxLength;
    }

    StopWordSet()
    {
        _maxLength = -1;
        _tokens = null;
    }

    private final Set<CharBuffer> _tokens;
    private final int _maxLength;
}
