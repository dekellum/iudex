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
package iudex.feed.rome;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.List;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import iudex.core.Content;
import iudex.core.ContentKeys;
import iudex.core.ContentSource;
import iudex.core.ParseException;
import iudex.core.VisitURL;
import iudex.core.VisitURL.SyntaxException;
import iudex.feed.FeedParser;

public class RomeFeedParser implements FeedParser
{
    @Override
    public Iterator<Content> parse( Content inContent ) 
        throws ParseException, IOException
    {
        try {
            ContentSource content = inContent.get( ContentKeys.CONTENT );
            if( content != null ) {
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = null;
                if( content.stream() != null ) {
                    XmlReader reader = 
                        new XmlReader( content.stream(), true, 
                                       content.defaultEncoding().name() );
                    feed = input.build( reader );
                }
                else if( content.characters() != null ) {
                    CharSequence source = content.characters();
                    Reader reader = null; 
                    if( source instanceof CharBuffer ) {
                        CharBuffer inBuff = (CharBuffer) source;
                        if( inBuff.hasArray() ) {
                            reader = new CharArrayReader( 
                                inBuff.array(), 
                                inBuff.arrayOffset() + inBuff.position(),
                                inBuff.remaining() );
                        }
                    }
                    if( reader == null ) {
                        reader = new StringReader( source.toString() );
                    }
                    feed = input.build( reader );
                }
                else {
                    throw new IllegalArgumentException
                    ( "content source type [" + 
                      content.source().getClass().getName() +  
                      "] not supported" );
                }
                
                List<?> entries = feed.getEntries();
                return new EntryIterator( entries );
            }
        }
        catch( FeedException x ) {
            throw new ParseException( x );
        }

        return null; //FIXME.
    }

    private class EntryIterator implements Iterator<Content>
    {

        public EntryIterator( List<?> entries )
        {
            _iter = entries.iterator();
        }

        public boolean hasNext()
        {
            return _iter.hasNext();
        }

        public Content next()
        {
            SyndEntry se = (SyndEntry) _iter.next();
            Content c = new Content();
            c.set( ContentKeys.TITLE, se.getTitle() );
            c.set( ContentKeys.PUB_DATE, se.getPublishedDate() );
            //se.getDescription();
            // Or: se.getContents()
            
            try {
                c.set( ContentKeys.URL, VisitURL.normalize( se.getUri() ) );
                //FIXME: Or getLink()?  
            }   
            catch( SyntaxException x ) {
                throw new RuntimeException( x );
                //FIXME: A bit harsh, build array in advance?
            }
            return c;
        }

        public void remove()
        {
            throw new UnsupportedOperationException( "remove" );
        }
        private Iterator<?> _iter;
    }
}
