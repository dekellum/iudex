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

package iudex.http;

import java.util.List;

import com.gravitext.htmap.Key;

import iudex.core.ContentKeys;

public class HTTPKeys extends ContentKeys
{
    public static final Key<List<Header>> REQUEST_HEADERS =
        createListKey( "request_headers" );

    public static final Key<List<Header>> RESPONSE_HEADERS =
        createListKey( "response_headers" );
}
