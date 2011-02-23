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

import iudex.brutefuzzy.service.JMSConnector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionExecutor<T extends SessionState>
{
    public SessionExecutor( JMSConnector connector,
                            SessionStateFactory<T> factory )
    {
        _connector = connector;
        _factory = factory;

        BlockingOfferQueue queue = new BlockingOfferQueue( 1000 );

        //FIXME: Config depth, threads, etc.

        _execService =
            new ThreadPoolExecutor( 1, 1,
                                    1, TimeUnit.SECONDS,
                                    queue,
                                    new SessionThreadFactory() );
    }

    public void execute( SessionTask<T> task )
        throws JMSException, NamingException, InterruptedException
    {
        try {
            _execService.execute( task );

            //FIXME: Or possibly just log these, as would be the case
            //if added to queue?
        }
        catch( JMSRuntimeException x ) {
            Throwable cause = x.getCause();

            if( cause instanceof JMSException ) {
                throw (JMSException) cause;
            }
            else if( cause instanceof NamingException ) {
                throw (NamingException) cause;
            }
            else if( cause instanceof InterruptedException ) {
                throw (InterruptedException) cause;
            }
        }
    }

    public void shutdown()
    {
        _execService.shutdown();
    }

    public boolean awaitTermination( long timeout, TimeUnit unit )
        throws InterruptedException
    {
        return _execService.awaitTermination( timeout, unit );
    }

    static class SessionThread<T extends SessionState> extends Thread
    {
        public SessionThread( Runnable r,
                              JMSConnector connector,
                              SessionStateFactory<T> factory )
            throws InterruptedException, JMSException, NamingException
        {
            super( r );
            //FIXME: Add self incrementing thread name.
            Connection connection = connector.awaitConnection();
            _state = factory.createSessionState( connector.context(),
                                                 connection );
        }

        public void run()
        {
            try {
                super.run();
            }
            catch( JMSRuntimeException x ) {
                _log.warn( "Exit due to: ", x.getCause() );
            }
            finally {
                close();
            }
        }

        public T state()
        {
            return _state;
        }

        private void close()
        {
            if( _state != null ) {
                try {
                    _state.close();
                }
                catch( JMSException x ) {
                    _log.warn( "On close: ", x );
                }
            }
        }

        private final T _state;
        private final Logger _log = LoggerFactory.getLogger( getClass() );

    }

    static final class JMSRuntimeException
        extends RuntimeException
    {
        public JMSRuntimeException( Exception cause )
        {
            super( cause );
        }
    }

    private final class SessionThreadFactory implements ThreadFactory
    {
        @Override
        public Thread newThread( Runnable r )
        {
            try {
                return new SessionThread<T>( r, _connector, _factory );
            }
            catch( InterruptedException x ) {
                Thread.currentThread().interrupt();
                throw new JMSRuntimeException( x );
            }
            catch( JMSException x ) {
                throw new JMSRuntimeException( x );
            }
            catch( NamingException x ) {
                throw new JMSRuntimeException( x );
            }
        }
    }

    private static final class BlockingOfferQueue
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
    private final ExecutorService _execService;
    private final JMSConnector _connector;
    private final SessionStateFactory<T> _factory;
}