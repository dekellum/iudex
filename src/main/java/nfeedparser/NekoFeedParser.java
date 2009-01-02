package nfeedparser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.cyberneko.html.parsers.SAXParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class NekoFeedParser extends DefaultHandler 

{
    public void parse( InputStream markup, 
                       Charset defaultCharset,
                       boolean restartOnChange ) throws ParseException 
    {
        XMLReader reader = new SAXParser();
        _restartOnChange = restartOnChange;
        _defaultCharset = defaultCharset;
        try {
            reader.setProperty( 
                "http://cyberneko.org/html/properties/default-encoding", 
                defaultCharset );

            // reader.setFeature( 
            // "http://cyberneko.org/html/features/scanner/ignore-specified-charset",
            //      true );
        }
        catch( SAXNotRecognizedException x ) {
            throw new RuntimeException( x );
        }
        catch( SAXNotSupportedException x ) {
            throw new RuntimeException( x );
        }
        reader.setContentHandler( this );
        
        InputSource inSource = new InputSource( markup );
        
        parse( inSource, reader );
    }

    private void parse( InputSource inSource, XMLReader reader )
        throws ParseException
    {
        try {
            reader.parse( inSource );
        }
        catch( IOException e ) {
            // Treat as Item rejected.
            throw new ParseException( e );
        }
        catch( SAXException e ) {
            // Treat as item rejected for now.
            throw new ParseException( e );
        }
        catch( ArrayIndexOutOfBoundsException x ) {
            // FIXME: Temp workaround for nekohtml-Bugs-1968435
            // Treat as item rejected case
            throw new ParseException( x );
        }
    }
    
    
    public void characters( char[] ch, int start, int length )
        throws SAXException
    {
        _log.debug( "chars: [{}]", new String( ch, start, length ) );
    }

    public void endDocument() throws SAXException
    {
        // TODO Auto-generated method stub
        
    }

    public void endElement( String uri, String localName, String name )
        throws SAXException
    {
        _log.debug( "</{}>", localName );
    }

    public void endPrefixMapping( String prefix ) throws SAXException
    {
        // TODO Auto-generated method stub
        
    }

    public void ignorableWhitespace( char[] ch, int start, int length )
        throws SAXException
    {
        // TODO Auto-generated method stub
        
    }

    public void processingInstruction( String target, String data )
        throws SAXException
    {
        // TODO Auto-generated method stub
        
    }

    public void setDocumentLocator( Locator locator )
    {
        // TODO Auto-generated method stub
        
    }

    public void skippedEntity( String name ) throws SAXException
    {
        // TODO Auto-generated method stub
        
    }

    public void startDocument() throws SAXException
    {
        // TODO Auto-generated method stub
        
    }

    public void startElement( String uri, String localName, String name,
                              Attributes atts ) throws SAXException
    {
        _log.debug( "<{}>", localName );
    }

    public void startPrefixMapping( String prefix, String uri )
        throws SAXException
    {
        // TODO Auto-generated method stub
        
    }               
    private boolean _restartOnChange;
    private Charset _defaultCharset;
    private static final Logger _log = 
        LoggerFactory.getLogger( NekoFeedParser.class );

}
