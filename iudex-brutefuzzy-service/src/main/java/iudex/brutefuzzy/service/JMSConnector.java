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

package iudex.brutefuzzy.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rjack.jms.JMSContext;

public class JMSConnector
    implements ExceptionListener, Runnable
{
    public JMSConnector( JMSContext context )
    {
        _context = context;
    }

    public synchronized void addConnectListener( ConnectListener listener )
    {
        _listeners.add( listener );
    }

    public JMSContext context()
    {
        return _context;
    }

    public int minConnectPoll()
    {
        return _minConnectPoll;
    }

    public void setMinConnectPoll( int minConnectPoll )
    {
        _minConnectPoll = minConnectPoll;
    }

    public int maxConnectPoll()
    {
        return _maxConnectPoll;
    }

    public void setMaxConnectPoll( int maxConnectPoll )
    {
        _maxConnectPoll = maxConnectPoll;
    }

    public int maxConnectDelay()
    {
        return _maxConnectDelay;
    }

    public void setMaxConnectDelay( int maxConnectDelay )
    {
        _maxConnectDelay = maxConnectDelay;
    }

    public void run()
    {
        try {
            connectLoop();
        }
        catch( JMSException x ) {
            _log.error( "Connection loop terminated with: ", x );
        }
        catch( NamingException x ) {
            _log.error( "Connection loop terminated with: ", x );
        }
    }

    public void connectLoop()
        throws JMSException, NamingException
    {
        try {
            _running = true;

            while( _running ) {
                connect();

                synchronized( this ) {
                    wait( 1000 );
                }
            }
        }
        catch( InterruptedException i ) {
            _log.warn( "In connectLoop:", i );
        }
        finally {
            _running = false;
        }
    }

    @Override
    public void onException( JMSException x )
    {
         if( _log.isDebugEnabled() ) _log.warn( "onException: ", x );
         else _log.warn( "onException: {}", x.toString() );

         synchronized( this ) {
             Connection connection = _connectRef.getAndSet( null );
             safeClose( connection );
             notifyAll();
         }
    }

    /**
     *  Called after createConnection, before connection.start,
     *  to notify ConnectListener's.
     */
    private void onConnect( Connection connection )
        throws JMSException, NamingException
    {
        for( ConnectListener listener : _listeners ) {
            listener.onConnect( _context, connection );
        }
    }

    private void connect()
        throws JMSException, NamingException, InterruptedException
    {
        long sleep = _minConnectPoll;
        long slept = 0;

        while( _connectRef.get() == null ) {
            Connection connection = null;
            try {
                connection = _context.createConnection();
                connection.setExceptionListener( this );

                // onConnect JMSException will be retried
                // NamingExpetion will not
                onConnect( connection );

                connection.start();

                _connectRef.set( connection );
                connection = null;
            }
            catch( JMSException x ) {
                if( slept < _maxConnectDelay ) {

                    if( _log.isDebugEnabled() ) _log.warn( "On connect:", x );
                    else _log.warn( "On connect: {}", x.toString() );

                    long s = Math.min( sleep, _maxConnectDelay - slept );
                    _log.info( "Sleeping for {}ms before next connect attempt",
                               s );
                    Thread.sleep( s );
                    slept += s;
                    sleep = Math.min( sleep * 2, _maxConnectPoll );
                }
                else throw x;
            }
            finally {
                safeClose( connection ); // If an unset connection remains:
                _context.close();
            }
        }
    }

    private void safeClose( Connection connection )
    {
        try {
            if( connection != null ) connection.close();
        }
        catch( JMSException x ) {
            if( _log.isDebugEnabled() ) {
                _log.warn( "On connection close: ", x );
            }
            else _log.warn( "On connection close: {}", x.toString() );
        }
    }

    private final JMSContext _context;
    private final Logger _log = LoggerFactory.getLogger( getClass() );

    private int _minConnectPoll  =    16; //ms
    private int _maxConnectPoll  =  2048;
    private int _maxConnectDelay = 30704;

    private volatile boolean _running = false;

    private AtomicReference<Connection> _connectRef
        = new AtomicReference<Connection>( null );

    private List<ConnectListener> _listeners =
        new ArrayList<ConnectListener>();
}
