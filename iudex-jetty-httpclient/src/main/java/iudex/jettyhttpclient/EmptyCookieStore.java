/*
 * Copyright (c) 2012 David Kellum
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

package iudex.jettyhttpclient;

import java.util.Collections;
import java.util.List;

import org.eclipse.jetty.client.api.CookieStore;
import org.eclipse.jetty.client.api.Destination;
import org.eclipse.jetty.http.HttpCookie;

/**
 * Temporary store for manually disabling cookies. The M4 release will have:
 *
 * jetty.util.HttpCookieStore.Empty
 *
 * ...for the same purpose.
 */
public class EmptyCookieStore implements CookieStore
{
    public EmptyCookieStore()
    {
    }

    @Override
    public List<HttpCookie> findCookies( Destination destination, String path )
    {
        return Collections.emptyList();
    }

    @Override
    public boolean addCookie( Destination destination, HttpCookie cookie )
    {
        return false;
    }

    @Override
    public void clear()
    {
        // No-op
    }
}
