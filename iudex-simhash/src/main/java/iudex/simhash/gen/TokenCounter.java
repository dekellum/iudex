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

import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.gravitext.util.CharSequences;

/**
 * Accumulates token counts from tokenized char/byte additions, and provides a
 * simhash summary.
 */
public final class TokenCounter
{
    public TokenCounter()
    {
        this( StopWordSet.EMPTY_SET );
    }

    public TokenCounter( StopWordSet stopwords )
    {
        _stopwords = stopwords;
    }

    public void add( CharSequence chars )
    {
        add( CharSequences.asCharBuffer( chars ) );
    }

    public void add( CharBuffer chars )
    {
        add( new Tokenizer( chars.duplicate() ) );
    }

    public void add( Iterator<CharBuffer> tokens )
    {
        while( tokens.hasNext() ) {

            // String looks are bit faster, so copy to String here.
            final String token = tokens.next().toString();

            if( ! _stopwords.contains( token ) ) {
                Counter count = _counts.get( token );
                if( count == null ) {
                    count = new Counter();
                    _counts.put( token, count );
                }
                count.incr();
            }
        }
    }

    public long simhash()
    {
        SimHasher sh = new SimHasher();
        for( Entry<String, Counter> e : _counts.entrySet() ) {
            sh.addFeature( MurmurHash64.hash( e.getKey() ),
                           e.getValue().count );
        }

        return sh.summarize();
    }

    private static final class Counter
    {
        void incr()
        {
            ++count;
        }

        int count = 0;
    }

    private final StopWordSet _stopwords;

    private final Map<String,Counter> _counts =
        new HashMap<String,Counter>( 2048 );
}
