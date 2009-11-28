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
package iudex.core;

import java.io.IOException;
import java.util.Iterator;

import com.gravitext.htmap.UniMap;

/**
 * Generic interface for parsing components, such as feed, sitemap, and
 * page parsers.
 * @author David Kellum
 */
public interface ContentParser
{
    /**
     * Parse the given content, returning iterator to obtain child references.
     * @return Iterator over (0..n) child references
     * @throws IOException from reading {@link ContentSource@#stream()}
     * @throws ParseException from other parse specific problem.
     */
    public Iterator<UniMap> parse( UniMap content )
        throws ParseException, IOException;
}
