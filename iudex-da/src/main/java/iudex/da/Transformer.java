/*
 * Copyright (c) 2008-2014 David Kellum
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

package iudex.da;

import com.gravitext.htmap.UniMap;

/**
  * Transform content and references during insert/update operations. Supports
  * late revisions based on database current state.
 */
public interface Transformer
{
    /**
     * Transform content to be inserted/updated.
     * @param updated content to be written
     * @param current from database copy or null if not found. Should not be
     * modified.
     * @return content to write/update
     */
    UniMap transformContent( UniMap updated, UniMap current );

    /**
     * Transform referer of content to be inserted/updated.
     * @param updated referer of content to be written
     * @param current from database copy or null if not found. Should not be
     * modified.
     * @return content to write/update
     */
    UniMap transformReferer( UniMap update, UniMap current );

    /**
     * Transform a reference to be inserted/updated.
     * @param updated reference to be written
     * @param current from database copy or null if not found. Should not
     * be modified.
     * @return content to write/update
     */
    UniMap transformReference( UniMap updated, UniMap current );
}
