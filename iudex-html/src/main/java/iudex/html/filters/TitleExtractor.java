/*
 * Copyright (c) 2010-2011 David Kellum
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

package iudex.html.filters;

import java.util.Arrays;
import java.util.List;

import com.gravitext.htmap.Key;
import com.gravitext.htmap.UniMap;
import com.gravitext.xml.tree.Element;

import iudex.core.ContentKeys;
import iudex.filter.Described;
import iudex.filter.Filter;
import iudex.filter.FilterException;
import iudex.html.HTML;
import iudex.html.HTMLKeys;

public class TitleExtractor implements Filter, Described
{
    public TitleExtractor()
    {
        this( HTMLKeys.SOURCE_TREE, ContentKeys.TITLE );
    }

    public TitleExtractor( Key<Element> tree, Key<CharSequence> title )
    {
        _treeKey = tree;
        _titleKey = title;
    }

    @Override
    public List<?> describe()
    {
        return Arrays.asList( (Key) _treeKey, _titleKey );
    }

    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        Element root = content.get( _treeKey );
        content.remove( _titleKey );

        if( root != null ) {
            if( root.tag().equals( HTML.HTML ) ) {
                Element title = root.firstElement( HTML.HEAD, HTML.TITLE );
                if( title != null ) {
                    content.set( _titleKey, title.characters() );
                }
            }
        }
        return true;
    }

    private final Key<Element> _treeKey;
    private final Key<CharSequence> _titleKey;
}
