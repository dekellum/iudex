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


/**
 * VisitURL plus: type (intended use), priority
 *  
 */
public class VisitOrder implements Comparable<VisitOrder>
{
    public static enum Type {
        FEED,
        PAGE,
        ROBOTS_TXT,
        SITE_MAP
    }

    public VisitOrder( VisitURL url,
                       Type type,
                       Double priority )
    {
        _url = url;
        _type = type;
        _priority = priority;
    }

    public VisitURL url()
    {
        return _url;
    }

    public Type type()
    {
        return _type;
    }

    public Double priority()
    {
        return _priority;
    }

    /**
     * Order by descending priority.
     */
    public int compareTo( VisitOrder o )
    {
        return Double.compare( o.priority(), this.priority() );
    }

    private final VisitURL _url;
    private final Type _type;
    private final Double _priority;
}
