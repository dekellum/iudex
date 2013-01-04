/*
 * Copyright (c) 2008-2013 David Kellum
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

/**
 * A domain key with an optional type, if HostQueue type specificity is
 * desired.
 */
public final class DomainKey
{
    public DomainKey( String domain, String type )
    {
        _domain = domain;
        _type = type;
    }

    public String domain()
    {
        return _domain;
    }

    public String type()
    {
        return _type;
    }

    @Override
    public int hashCode()
    {
        int hc = _domain.hashCode();
        if( _type != null ) hc ^= _type.hashCode();
        return hc;
    }

    @Override
    public boolean equals( Object other )
    {
        if( other == this ) return true;

        if( other instanceof DomainKey ) {
            DomainKey o = (DomainKey) other;
            return ( _domain.equals( o._domain ) &&
                     ( ( _type == o._type ) ||
                       ( ( _type != null ) &&
                           _type.equals( o._type ) ) ) );
        }
        return false;
    }

    @Override
    public String toString()
    {
        if( _type == null ) {
            return _domain;
        }
        return _domain + ':' + _type;
    }

    private final String _domain;
    private final String _type;
}
