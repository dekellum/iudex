/*
 * Copyright (c) 2008-2011 David Kellum
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

package iudex.util;

import java.nio.CharBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer logs each write block to a given SLF4J writer at DEBUG level.
 */
public final class LogWriter
    extends java.io.Writer
{
    public LogWriter( String name )
    {
        _log = LoggerFactory.getLogger( name );
    }

    /**
     * Set a pattern to use for removing text from each write call.
     */
    public void setRemovePattern( Pattern pattern )
    {
        _removePattern = pattern;
    }

    @Override
    public void close()
    {
        //No-op
    }

    @Override
    public void flush()
    {
        //No-op
    }

    @Override
    public void write( final char[] buffer, final int offset, int length )
    {
        if( ! _log.isDebugEnabled() ) return;

        String msg = null;
        if( _removePattern != null ) {
            CharBuffer b = CharBuffer.wrap( buffer, offset, length );
            Matcher m = _removePattern.matcher( b );
            msg = m.replaceAll( "" );
        }
        else {
            msg = String.valueOf( buffer, offset, length );
        }

        if( msg.length() > 0 ) _log.debug( msg );
    }

    private final Logger _log;
    private Pattern _removePattern = null;

}
