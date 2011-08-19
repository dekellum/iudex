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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MojiBakeMapper
{
    public MojiBakeMapper( String regex,
                           Map<String,String> mojis )
    {
        _mojiPattern = Pattern.compile( regex );
        _mojis = new HashMap<String,String>( mojis );
    }

    public CharSequence recover( CharSequence in )
    {
        Matcher m = _mojiPattern.matcher( in );
        StringBuilder out = new StringBuilder( in.length() );
        int last = 0;
        while( m.find() ) {
            out.append( in, last, m.start() );
            String moji = in.subSequence( m.start(), m.end() ).toString();
            out.append( _mojis.get( moji ) );
            last = m.end();
        }
        out.append( in, last, in.length() );

        if( out.length() < in.length() ) {
            return recover( out );
        }
        else {
            return out;
        }
    }

    private final Pattern _mojiPattern;
    private final HashMap<String, String> _mojis;
}
