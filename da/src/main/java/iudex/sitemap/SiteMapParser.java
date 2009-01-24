package iudex.sitemap;

import iudex.core.Content;
import iudex.core.ParseException;

import java.util.Iterator;

/**
 * @see http://www.sitemaps.org/protocol.php
 */
public interface SiteMapParser
{
    
    public Iterator<Content> parse( Content sitemap ) throws ParseException;
    
    /*
     * Sitemap input: CONTENT_SOURCE, URL, Content-Type, etc.
     * (either an index or a sitemap)
     * Returns list of SITEMAP(or FEED) or individual PAGE references, each 
     * with:
     * (URL, LAST_MODIFIED_DATE (?), CHANGE_FREQUENCY, SITEMAP_PRIORITY)
     */
}
