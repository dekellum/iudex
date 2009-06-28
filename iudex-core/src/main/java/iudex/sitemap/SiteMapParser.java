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
package iudex.sitemap;

import iudex.core.ParseException;

import java.util.Iterator;

import com.gravitext.htmap.UniMap;

/**
 * @see http://www.sitemaps.org/protocol.php
 */
public interface SiteMapParser
{

    public Iterator<UniMap> parse( UniMap sitemap ) throws ParseException;

    /*
     * Sitemap input: CONTENT_SOURCE, URL, Content-Type, etc.
     * (either an index or a sitemap)
     * Returns list of SITEMAP(or FEED) or individual PAGE references, each
     * with:
     * (URL, LAST_MODIFIED_DATE (?), CHANGE_FREQUENCY, SITEMAP_PRIORITY)
     */
}
