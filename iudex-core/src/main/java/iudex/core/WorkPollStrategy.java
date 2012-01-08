/*
 * Copyright (c) 2008-2012 David Kellum
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

package iudex.core;

public interface WorkPollStrategy
{
    /**
     * Indicate if work should be polled or when the next check should
     * be made.
     * @param current the current VisitQueue being processed
     * @param elapsed milliseconds since last poll
     * @return suggested milliseconds before next check or
     * less than or equal to zero to check now.
     */
    long nextPollWork( VisitQueue current, long elapsed );

    /**
     * Indicates if visitors on the current VisitQueue should be
     * shutdown and a new VisitQueue created. If true, the next call
     * to pollWork should return a new queue.
     */
    boolean shouldReplaceQueue( VisitQueue current );

    /**
     * Add to current or return new VisitQueue with new work.
     * @param current VisitQueue or null, requiring new queue.
     * @return existing or new VisitQueue
     */
    VisitQueue pollWork( VisitQueue current );
}
