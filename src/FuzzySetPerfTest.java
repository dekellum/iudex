import java.util.Random;

import com.gravitext.concurrent.TestFactory;
import com.gravitext.concurrent.TestRunnable;

public class FuzzySetPerfTest implements TestFactory
{
    public String name()
    {
        return "FuzzySet64";
    }

    public TestRunnable createTestRunnable( final int seed )
    {
        return new TestRunnable() {

            {
                // Pre define a random set of long keys
                Random random = new Random( seed );
                int length = 100;
                _testKeys = new long[length];
                for( int i = 0; i < length; i++ ) {
                    _testKeys[i] = random.nextLong();
                }
            }

            public int runIteration( int run )
            {
                final int end = _testKeys.length;
                final FuzzySet64 set = new FuzzySet64( end, 6 );
                int hits = 0;
                for( int i = 0; i < end; ++i ) {
                    if( ! set.add( _testKeys[i] ) ) ++hits;
                }
                return hits;
            }

            final long _testKeys[];
        };
    }
}
