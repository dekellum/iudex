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
        _thresholdBits = thresholdBits;
        _testKeys = BruteFuzzy.testKeys( length, thresholdBits, 666 );
    }

    public String name()
    {
        String name = _mode.name();
        if( _mode == Mode.TREE ) {
            name += "-" + _maxBits;
        }
        return name;
    }

    public void setMaxBits( int maxBits )
    {
        _maxBits = maxBits;
    }

    public void setInitialCapacity( int initialCapacity )
    {
        _initialCapacity = initialCapacity;
    }

    public TestRunnable createTestRunnable( final int seed )
    {
        switch( _mode ) {
        case LIST:
            return new BaseRunnable() {
                FuzzySet64 createSet64( final int cap )
                {
                    return new FuzzyList64( cap, _thresholdBits );
                }
            };
        case TREE:
            return new BaseRunnable() {
                FuzzySet64 createSet64( final int cap )
                {
                    return new FuzzyTree64( cap, _thresholdBits, _maxBits );
                }
            };
        }
        throw new IllegalStateException();
    }

    private abstract class BaseRunnable implements TestRunnable
    {
        public final int runIteration( int run )
        {
            final int end = _testKeys.length;
            int cap = ( _initialCapacity >= 0 ) ? _initialCapacity : end;
            final FuzzySet64 set = createSet64( cap );
            int hits = 0;
            for( int i = 0; i < end; ++i ) {
                if( ! set.add( _testKeys[i] ) ) ++hits;
            }
            return hits;
        }
        abstract FuzzySet64 createSet64( final int end );

   };

    private final Mode _mode;
    private final int _thresholdBits;
    private int _maxBits = 8;
    private int _initialCapacity = -1;
    private final long _testKeys[];
}
