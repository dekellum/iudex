/*
 * Copyright (c) 2011 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package iudex.brutefuzzy.client;

import iudex.brutefuzzy.service.BaseService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import rjack.jms.JMSContext;

public class BaseClientExecutor extends BaseService
{
    public class SessionThread extends Thread
    {
        public SessionThread( Runnable r )
        {
            super( r );
        }

        public void run()
        {
            try {
                super.run();
            }
            catch( RuntimeException x ) {
                //FIXME: Log?
                throw x;
            }
            finally {
                close();
            }
        }

        public Session session() throws JMSException
        {
            if( _session == null ) {
                _session = context().createSession( connection() );
            }
            return _session;
        }

        private void close()
        {

            if( _session != null ) {
                try {
                    _session.close();
                }
                catch( JMSException e ) {
                    //FIXME:
                }
                _session = null;
            }
        }

        private Session _session = null;
    }

    public BaseClientExecutor( JMSContext context )
    {
        super( context );

        BlockingOfferQueue queue = new BlockingOfferQueue( 1000 );
        //FIXME: Config.
        _execService =
            new ThreadPoolExecutor( 5, 10,
                                    1, TimeUnit.SECONDS,
                                    queue,
                                    new SessionThreadFactory() );

    }

    public Connection connection()
    {
        // TODO Auto-generated method stub
        return null;
    }

    private final class SessionThreadFactory implements ThreadFactory
    {
        @Override
        public Thread newThread( Runnable r )
        {
            return new SessionThread( r );
        }
    }

    public final static class BlockingOfferQueue
        extends LinkedBlockingQueue<Runnable>
    {
        public BlockingOfferQueue( int capacity )
        {
            super( capacity );
        }

        @Override
        public boolean offer( Runnable e )
        {
            try {
                put( e );
                return true;
            }
            catch( InterruptedException x ) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private ExecutorService _execService;
}
