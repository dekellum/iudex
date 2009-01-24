package iudex.feed;

import iudex.core.Content;
import iudex.core.ParseException;

import java.util.Iterator;

public interface FeedParser
{
    public Iterator<Content> parse( Content feed ) throws ParseException;
    // parse input: 
    // IOStream, Chars (Bytes)
    // Content-Type
    // Feed URL
    // Other feed information.. (priorities, etc.?)
    
}
