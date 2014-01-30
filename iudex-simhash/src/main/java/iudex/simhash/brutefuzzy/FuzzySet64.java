/*
 * Copyright (c) 2010-2014 David Kellum
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

package iudex.simhash.brutefuzzy;

import java.util.Collection;

/**
 * Interface for a set-like container of 64-bit keys.
 */
public interface FuzzySet64
{
    /**
     * Add key if no matching keys are present.
     * @return true if the key was added.
     */
    boolean addIfNotFound( final long key );

    /**
     * Find all hamming-distance matches from the specified key.
     * @param key to find
     * @param matches to which all existing matching keys are added
     * @return true if an exact match was found.
     */
    boolean findAll( final long key, Collection<Long> matches );

    /**
     * Find all existing hamming-distance matches and add the current key.
     * @param key to find and add
     * @param matches to which all existing matching keys are added
     * @return true if an exact match was found.
     */
    boolean addFindAll( final long key, Collection<Long> matches );

    /**
     * Remove the specified (exact) key if present.
     * @param key to remove
     * @return true if the specified key was found and removed.
     */
    boolean remove( final long key );
}
