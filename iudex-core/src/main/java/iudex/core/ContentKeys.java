/*
 * Copyright (c) 2008-2010 David Kellum
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
package iudex.core;

import java.util.Date;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;

public class ContentKeys
{
    public static final Key<VisitURL> URL =
        create( "url", VisitURL.class );

    /**
     * Registration level domain extracted from URL's host.
     */
    public static final Key<String> RL_DOMAIN =
        create( "rl_domain", String.class );

    public static final Key<String> TYPE =
        create( "type", String.class );

    public static final String TYPE_FEED    = "FEED";
    public static final String TYPE_PAGE    = "PAGE";
    public static final String TYPE_ROBOTS  = "ROBOTS";
    public static final String TYPE_SITEMAP = "SITEMAP";

    public static final Key<Date> LAST_VISIT =
        create( "last_visit", Date.class );

    public static final Key<Integer> STATUS =
        create( "status", Integer.class );

    public static final Key<String> REASON =
        create( "reason", String.class );

    public static final String REASON_DUPE = "DUPE";

    public static final Key<CharSequence> TITLE =
        create( "title", CharSequence.class );

    public static final Key<CharSequence> EXTRACT =
        create( "extract", CharSequence.class );

    public static final Key<CharSequence> SUMMARY =
        create( "summary", CharSequence.class );

    public static final Key<CharSequence> CONTENT =
        create( "content", CharSequence.class );

    public static final Key<ContentSource> SOURCE =
        create( "source", ContentSource.class );

    public static final Key<Date> REF_PUB_DATE =
        create( "ref_pub_date", Date.class );

    public static final Key<Date> PUB_DATE =
        create( "pub_date", Date.class );

    /**
     * Difference between this REF_PUB_DATE and CURRENT.REF_PUB_DATE
     * in seconds.
     */
    public static final Key<Float> REF_PUB_DELTA =
        create( "ref_pub_delta", Float.class );

    /**
     * Highest priority wins.
     */
    public static final Key<Float> PRIORITY =
        create( "priority", Float.class );

    public static final Key<Date> NEXT_VISIT_AFTER =
        create( "next_visit_after", Date.class );

    /**
     * Start of processing. Inherited from REFERER on REFERENCES. Used as
     * "now" in most processing contexts.
     */
    public static final Key<Date> VISIT_START =
        create( "visit_start", Date.class );

    public static final Key<List<UniMap>> REFERENCES =
        createListKey( "references" );

    /**
     * Number of references that are new.
     */
    public static final Key<Integer> NEW_REFERENCES =
        create( "new_references", Integer.class );

    /**
     * Number of references that are updated.
     */
    public static final Key<Integer> UPDATED_REFERENCES =
        create( "updated_references", Integer.class );

    public static final Key<UniMap> REFERER =
        create( "referer", UniMap.class );

    public static final Key<UniMap> REFERENT =
        create( "referent", UniMap.class );

    /**
     * The current state of content during a transform/update operation.
     */
    public static final Key<UniMap> CURRENT =
        create( "current", UniMap.class );

    public static final Key<Integer> CACHE_FILE =
        create( "cache_file", Integer.class );

    public static final Key<Long> CACHE_FILE_OFFSET =
        create( "cache_file_offset", Long.class );

    protected static <T> Key<T> create( String name, Class<T> valueType )
    {
        return UniMap.KEY_SPACE.create( name, valueType );
    }

    protected static <T> Key<List<T>> createListKey( String name )
    {
        return UniMap.KEY_SPACE.createListKey( name );
    }
}
