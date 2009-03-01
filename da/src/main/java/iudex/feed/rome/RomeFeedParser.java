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
            c.set( ContentKeys.PUBLISHED_DATE, se.getPublishedDate() );
            //se.getDescription();
            // Or: se.getContents()
            
            try {
                c.set( ContentKeys.URL, VisitURL.normalize( se.getUri() ) );
                //FIXME: Or getLink()?  
            }   
            catch( SyntaxException x ) {
                x.printStackTrace(); // FIXME:
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
