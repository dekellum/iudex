/*
 * Copyright (c) 2008-2015 David Kellum
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

public final class Domains
{

    /**
     * Return the registration level domain (public domain suffix plus one) for
     * the given host name if one is found, or null otherwise.
     */
    public static String registrationLevelDomain( String name )
    {
        name = normalize( name );

        String child = null;
        while( name != null ) {
            if( TLDSets.KNOWN_TLDS.contains( name ) ) return child;

            final String parent = parent( name );

            if( TLDSets.TLD_PARENTS.contains( parent ) ) {
                return TLDSets.REG_EXCEPTIONS.contains( name  ) ? name : child;
            }

            child = name;
            name = parent;
        }
        return null;
    }

    public static String normalize( String name )
    {
        if( name != null ) {
            name = name.toLowerCase();
            if( name.isEmpty() ) {
                name = null;
            }
            else if( name.charAt( name.length() - 1 ) == '.' ) {
                if( name.length() == 1 ) {
                    name = null;
                }
                else {
                    name = name.substring( 0, name.length() - 1 );
                }
            }
        }
        return name;
    }

    static String parent( final String name )
    {
        int fdot = name.indexOf( '.' );

        return ( ( ( fdot >= 0 ) && ( fdot + 1 < name.length() ) ) ?
                   name.substring( fdot + 1 ) : null );
    }
}
