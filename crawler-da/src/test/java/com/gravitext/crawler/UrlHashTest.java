package com.gravitext.crawler;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import static org.junit.Assert.*;

public class UrlHashTest
{
    @Test
    public void testHashURL()
    {
        CharSequence hash = URLHash.hashURL( "http://gravitext.com" );
        assertEquals( 23, hash.length() );

        CharSequence ohash = URLHash.hashURL( "http://gravitext.com/blog/x/y" );
        assertFalse( hash.toString().equals( ohash.toString() ) );
    }
    
    @Test
    public void testHashDomain()
    {
        CharSequence hash = URLHash.hashDomain( "gravitext.com" );
        assertEquals( 6, hash.toString().length() );
        CharSequence ohash = URLHash.hashURL( "other.com" );
        assertFalse( hash.toString().equals( ohash.toString() ) );
    }
    
    @Test
    public void testNormalize() throws URISyntaxException
    {
        assertNormal( "http://gravitext.com/foo", 
                      "HTTP://GRAVITEXT.com:80/foo" );

        assertNormal( "http://gravitext.com/foo/", 
                      "HTTP://gravitext.com/bar/../foo/" );
        
        assertNormal( "http://gravitext.com/foo%20bar", 
                      "HTTP://gravitext.com/%66oo%20bar" );
        
        assertNormal( "http://gravitext.com/f%C5%8Do", 
                      "HTTP://gravitext.com/fōo" );
              
        assertNormal( "http://gravitext.com/foo", 
                      "http://gravitext.com/foo#anchor" );

        assertNormal( "http://gravitext.com/foo?query=a+b", 
                      "HTTP://gravitext.com/foo?query=a+b#anchor" );

    }
    
    public void assertNormal( String norm, String orig ) 
        throws URISyntaxException
    {
        assertEquals( norm, 
                      URLHash.normalize( new URI( orig ) ).toString() );
    }
   
}
    