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

public class VisitQueueFactory
{
    public VisitQueueFactory( VisitQueue template )
    {
        _template = template.clone();
    }

    public VisitQueueFactory()
    {
        _template = new VisitQueue();
    }

    public VisitQueue createVisitQueue()
    {
        return _template.clone();
    }

    private final VisitQueue _template;
}
