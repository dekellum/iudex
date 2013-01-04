/*
 * Copyright (c) 2008-2013 David Kellum
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
package iudex.http;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseResponseHandler implements ResponseHandler
{

    @Override
    public void sessionCompleted( HTTPSession session )
    {
        try {
            sessionCompletedUnsafe( session );
        }
        catch( IOException x ) {
            _log.warn( "Url: {} :: {}",  session.url(), x.toString() );
            _log.debug( "Stack dump: ", x );
        }
        finally {
            session.close();
        }
    }

    protected void sessionCompletedUnsafe( HTTPSession session )
        throws IOException
    {
    }

    private static final Logger _log =
         LoggerFactory.getLogger( BaseResponseHandler.class );
}
