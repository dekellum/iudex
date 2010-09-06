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

import static com.gravitext.util.Charsets.UTF_8;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Accumulates token counts from tokenized char/byte additions, and provides a
 * simhash summary.
 */
public final class TokenCounter
{
    public void add( String chars )
    {
        add( UTF_8.encode( chars ) );
    }

    public void add( CharBuffer chars )
    {
        add( UTF_8.encode( chars ) );
    }

    public void add( ByteBuffer bytes )
    {
        add( new ByteTokenizer( bytes ) );
    }

    public void add( Iterator<ByteBuffer> tokens )
    {
        while( tokens.hasNext() ) {
            final ByteBuffer token = tokens.next();
            Counter count = _counts.get( token );
            if( count == null ) {
                count = new Counter();
                _counts.put( token, count );
            }
            count.incr();
        }
    }

    public long simhash()
    {
        SimHasher sh = new SimHasher();
        for( Entry<ByteBuffer, Counter> e : _counts.entrySet() ) {
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

    private final Map<ByteBuffer,Counter> _counts =
        new HashMap<ByteBuffer,Counter>( 2048 );
}