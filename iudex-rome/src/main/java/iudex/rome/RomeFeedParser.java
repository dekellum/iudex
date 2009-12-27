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
package iudex.rome;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gravitext.htmap.UniMap;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import com.sun.syndication.io.XmlReaderException;

import static iudex.core.ContentKeys.*;

import iudex.core.ContentSource;
import iudex.core.VisitURL;
import iudex.core.VisitURL.SyntaxException;
import iudex.filter.Filter;
import iudex.filter.FilterException;

/**
 * Uses ROME to parse feed CONTENT and sets REFERENCES accordingly.
 *
 * Missing PUB_DATE,TITLE,URL is allowed in references (should be resolved by
 * down-stream filters.)
 */
public class RomeFeedParser implements Filter
{
    @Override
    public boolean filter( UniMap content ) throws FilterException
    {
        ContentSource src = content.get( CONTENT );
        if( src != null ) {
            try {
                Reader reader = contentReader( src );

                parse( reader, content );
                return true;
            }
            catch( FeedException x ) {
                throw new FilterException( x );
            }
            catch( IOException x ) {
                throw new FilterException( x );
            }
        }

        return false;
    }

    private void parse( Reader reader, UniMap content ) throws FeedException
    {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build( reader );

        ArrayList<UniMap> refs = new ArrayList<UniMap>();
        List<?> entries = feed.getEntries();
        for( Object oe : entries ) {
            SyndEntry entry = (SyndEntry) oe;

            refs.add( entryToReference( entry ) );
        }
        content.set( REFERENCES, refs );
    }

    private UniMap entryToReference( SyndEntry entry )
    {
        UniMap ref = new UniMap();
        try {
            if( entry.getLink() != null ) {
                //FIXME: Handle URLs relative to feed.
                ref.set( URL, VisitURL.normalize( entry.getLink() ) );
            }
        }
        catch( SyntaxException x ) {
            _log.warn( "On link {}: {}", entry.getLink(), x.getMessage() );
        }

        ref.set( TITLE, entry.getTitle() );

        Date bestDate = maxDateOrNull( entry.getPublishedDate(),
                                       entry.getUpdatedDate() );
        ref.set( REF_PUB_DATE, bestDate );
        ref.set( PUB_DATE,     bestDate );

        //FIXME: se.getDescription() or se.getContents()?

        return ref;
    }

    private Date maxDateOrNull( Date d1, Date d2 )
    {
        Date d = null;
        if( d1 != null ) {
            if( d2 != null ) {
                d = ( d1.compareTo( d2 ) >= 0 ) ? d1 : d2;
            }
            else {
                d = d1;
            }
        }
        else if( d2 != null ) {
            d = d2;
        }
        return d;
    }

    /**
     * Return appropriate Reader for the ContentSource.
     */
    private Reader contentReader( ContentSource content )
        throws IOException, XmlReaderException
    {
        Reader reader = null;
        if( content.stream() != null ) {
            reader = new XmlReader( content.stream(), true,
                                    content.defaultEncoding().name() );
            //FIXME: Or pass raw content-type?
        }
        else if( content.characters() != null ) {
            CharSequence source = content.characters();
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
        }
        else {
            throw new IllegalArgumentException
            ( "content source type [" +
              content.source().getClass().getName() +
              "] not supported" );
        }
        return reader;
    }

    private Logger _log = LoggerFactory.getLogger( getClass() );
}
