/*
* Copyright (c) 2008-2015 David Kellum
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

package iudex.core.filters;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.util.MojiBakeMapper;

public class MojiBakeFilter
    implements Filter, Described
{
    public MojiBakeFilter( Key<CharSequence> field,
                           String regex,
                           Map<String,String> mojis )
    {
        _field = field;
        _mapper = new MojiBakeMapper( regex, mojis );
    }

    @Override
    public List<?> describe()
    {
        return Arrays.asList( _field.toString() );
    }

    @Override
    public boolean filter( UniMap content )
    {
        CharSequence in = content.get( _field );
        if( in != null ) {
            content.set( _field, _mapper.recover( in ) );
        }

        return true;
    }

    private final Key<CharSequence> _field;
    private final MojiBakeMapper _mapper;
}
