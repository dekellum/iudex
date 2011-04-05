/*
 * Copyright (c) 2010-2011 David Kellum
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

import iudex.simhash.SimHashKeys;

import com.gravitext.concurrent.TestFactory;
import com.gravitext.concurrent.TestRunnable;
import com.gravitext.htmap.UniMap;

public class SimHashGenPerfTest implements TestFactory
{
    public SimHashGenPerfTest( UniMap input, SimHashGenerator generator )
    {
        _input = input;
        _generator = generator;
    }

    @Override
    public String name()
    {
        return "SimHashGen";
    }

    @Override
    public TestRunnable createTestRunnable( int seed )
    {
        return new GenRunner();
    }

    private final class GenRunner implements TestRunnable
    {

        @Override
        public int runIteration( int run ) throws Exception
        {
            final UniMap map = _input.clone();
            _generator.filter( map );

            return (int) ( map.get( SimHashKeys.SIMHASH ) & 0xffL );
        }
    }

    private final UniMap _input;
    private final SimHashGenerator _generator;
}
