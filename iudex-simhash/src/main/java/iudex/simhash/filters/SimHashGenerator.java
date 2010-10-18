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

package iudex.simhash.filters;

import java.util.ArrayList;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.filter.FilterException;
import iudex.simhash.SimHashKeys;
import iudex.simhash.gen.StopWordSet;
import iudex.simhash.gen.TokenCounter;

public class SimHashGenerator implements Filter, Described
{
    public static final class Input
    {
        public static Input forText( Key<CharSequence> text )
        {
            return new Input( text );
        }

        Input( Key<CharSequence> text )
        {
            textKey = text;
        }

        @Override
        public String toString()
        {
            return textKey.name();
        }

        Key<CharSequence> textKey;
    }

    public SimHashGenerator( List<Input> inputs )
    {
        this( inputs, StopWordSet.EMPTY_SET );
    }

    public SimHashGenerator( List<Input> inputs, StopWordSet stopWords )
    {
        _inputs = new ArrayList<Input>( inputs );
        _stopWords = stopWords;
    }

    @Override
    public List<?> describe()
    {
        return _inputs;
    }

    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        final TokenCounter counter = new TokenCounter( _stopWords );

        for( Input in : _inputs ) {
            if( in.textKey != null ) {
                CharSequence text = content.get( in.textKey );
                if( text != null ) counter.add( text );
            }
            else {
                // ...
            }
        }

        //Note: Set hash value zero for no input (i.e. the empty doc)
        content.set( SimHashKeys.SIMHASH, counter.simhash() );

        return true;
    }

    private final List<Input> _inputs;
    private final StopWordSet _stopWords;
}
