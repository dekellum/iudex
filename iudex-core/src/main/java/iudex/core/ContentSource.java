/*
 * Copyright (c) 2008-2014 David Kellum
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
package iudex.core;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import com.gravitext.util.Streams;

public class ContentSource
{
    public ContentSource( CharSequence seq )
    {
        _source = seq;
    }

    public ContentSource( ByteBuffer buffer )
    {
        _source = buffer;
    }

    public ContentSource( InputStream in )
    {
        _source = in;
    }

    public InputStream stream()
    {
        if( _source instanceof InputStream ) {
            return (InputStream) _source;
        }
        else if( _source instanceof ByteBuffer ) {
            return Streams.inputStream( (ByteBuffer) _source );
        }
        return null;
    }

    public CharSequence characters()
    {
        return ( ( _source instanceof CharSequence ) ?
                 (CharSequence) _source : null );
    }

    public boolean setDefaultEncoding( Charset encoding )
    {
        return setDefaultEncoding( encoding, 0.0F );
    }

    public boolean setDefaultEncoding( Charset encoding, float newConfidence )
    {
        Float oldConfObj = _encodings.get( encoding );
        float oldConf = ( oldConfObj != null ) ? oldConfObj : 0.0F;
        float conf = oldConf + newConfidence;
        _encodings.put( encoding, conf );

        if( ( conf > _encodingConfidence ) ||
            ( _encodingConfidence == 0.0F ) ) {
            _defaultEncoding = encoding;
            _encodingConfidence = conf;
            return true;
        }
        return false;
    }

    public boolean setDefaultEncoding( Map<Charset,Float> confidences )
    {
        boolean changed = false;

        for( Map.Entry<Charset, Float> e : confidences.entrySet() ) {
            boolean c = setDefaultEncoding( e.getKey(), e.getValue() );
            changed = changed || c;
        }

        return changed;
    }

    /**
     * Default encoding if set (i.e. HTTP Content-Type; charset hint) or null
     * when the source is already characters.
     */
    public Charset defaultEncoding()
    {
        return _defaultEncoding;
    }

    public Object source()
    {
        return _source;
    }

    public float encodingConfidence()
    {
        return _encodingConfidence;
    }

    public Map<Charset,Float> encodingConfidences()
    {
        return _encodings;
    }

    private Object  _source;
    private Charset _defaultEncoding = null;
    private float   _encodingConfidence = 0.0F;

    private Map<Charset,Float> _encodings = new LinkedHashMap<Charset,Float>();
}
