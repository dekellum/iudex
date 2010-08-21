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

package brutefuzzy;

import java.util.Random;

import com.gravitext.concurrent.TestFactory;
import com.gravitext.concurrent.TestRunnable;

public class FuzzySetPerfTest implements TestFactory
{
    public enum Mode
    {
        LIST,
        TREE
    };

    public FuzzySetPerfTest( Mode mode )
    {

        this( mode, 500, 6 );
    }

    public FuzzySetPerfTest( Mode mode, int length, int thresholdBits )
    {
        _mode = mode;
        _length = length;
        _thresholdBits = thresholdBits;
    }

    public String name()
    {
        return _mode.name();
    }

    public TestRunnable createTestRunnable( final int seed )
    {
        switch( _mode ) {
        case LIST:
            return new BaseRunnable( seed ) {
                FuzzySet64 createSet64( final int cap )
                {
                    return new FuzzyList64( cap, _thresholdBits );
                }
            };
        case TREE:
            return new BaseRunnable( seed ) {
                FuzzySet64 createSet64( final int cap )
                {
                    return new FuzzyTree64( cap, _thresholdBits );
                }
            };
        }
        throw new IllegalStateException();
    }

    private abstract class BaseRunnable implements TestRunnable
    {
        final long _testKeys[];

        BaseRunnable( final int seed )
        {
            // Pre define a random set of long keys
            Random random = new Random( seed );

            _testKeys = new long[_length];
            for( int i = 0; i < _length; i++ ) {
                _testKeys[i] = random.nextLong();
            }
        }

        public final int runIteration( int run )
        {
            final int end = _testKeys.length;
            final FuzzySet64 set = createSet64( end );
            int hits = 0;
            for( int i = 0; i < end; ++i ) {
                if( ! set.add( _testKeys[i] ) ) ++hits;
            }
            return hits;
        }
        abstract FuzzySet64 createSet64( final int end );

   };

    private final Mode _mode;
    private final int _length;
    private final int _thresholdBits;
}
