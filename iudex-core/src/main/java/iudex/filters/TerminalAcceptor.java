/*
 * Copyright (C) 2008-2009 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package iudex.filters;

import iudex.core.Filter;

import com.gravitext.htmap.UniMap;

/**
 * Added to the end of series of multiple FilterChains to pass the
 * accepted() event (all prior filters passed) to the specified
 * listener.
 *
 * @author David Kellum
 */
public class TerminalAcceptor implements Filter
{
    public TerminalAcceptor( FilterListener listener )
    {
        _listener = listener;
    }

    @Override
    public boolean filter( UniMap content )
    {
        _listener.accepted( content );
        return true;
    }

    private final FilterListener _listener;
}
