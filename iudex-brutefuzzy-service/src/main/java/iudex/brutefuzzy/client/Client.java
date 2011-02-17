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

import iudex.brutefuzzy.protobuf.ProtocolBuffers.Request;
import iudex.brutefuzzy.protobuf.ProtocolBuffers.Request.Builder;
import iudex.brutefuzzy.protobuf.ProtocolBuffers.RequestAction;
import iudex.brutefuzzy.protobuf.ProtocolBuffers.Response;

import java.util.concurrent.Semaphore;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rjack.jms.JMSContext;

import com.google.protobuf.InvalidProtocolBufferException;
import com.gravitext.util.Closeable;

public class Client
    implements MessageListener, Closeable, ExceptionListener
{
    public Client( JMSContext context )
        throws JMSException, NamingException
    {
        _connection = context.createConnection();

        _connection.setExceptionListener( this );

        _session = context.createSession( _connection );

        Destination requestQueue =
            context.lookupDestination( "iudex-brutefuzzy-request" );
        _producer = _session.createProducer( requestQueue );

        Destination responseQueue =
            context.lookupDestination( "iudex-brutefuzzy-listener" );

        _session.createConsumer( responseQueue ).setMessageListener( this );

        context.close();
    }

    public void open() throws JMSException
    {
        _connection.start();
    }

    public void close()
    {
        try {
            _connection.close();
        }
        catch( JMSException e ) {
            _log.warn(  "On Client.close: ", e );
        }

    }

    public void check( long simhash, boolean doAdd )
        throws JMSException, InterruptedException
    {
        //FIXME: Wrap JMSException to avoid leaking JMS dep?

        Builder bldr = Request.newBuilder();
        bldr.setSimhash( simhash );
        bldr.setAction( doAdd ? RequestAction.ADD : RequestAction.CHECK_ONLY );

        BytesMessage response = _session.createBytesMessage();
        response.writeBytes( bldr.build().toByteArray() );

        //FIXME: Replace with wait/notify to support a max wait
        _semaphore.acquire();

        _producer.send( response );
    }

    @Override
    public void onMessage( Message msg )
    {
        try {
            msg.acknowledge();
            if( msg instanceof BytesMessage ) {
                BytesMessage bmsg = (BytesMessage) msg;
                byte[] body = new byte[ (int) bmsg.getBodyLength() ];
                bmsg.readBytes( body );
                if( _log.isDebugEnabled() ) {
                    onResponse( Response.parseFrom( body ) );
                }
            }
            else {
                _log.error( "Received invalid message type: {}",
                            msg.getClass().getName() );
            }
        }
        catch( JMSException x ) {
            if( _log.isDebugEnabled() ) _log.error( "onMessage:", x );
            else _log.error( "onMessage: {}", x.toString() );
        }
        catch( InvalidProtocolBufferException x ) {
            if( _log.isDebugEnabled() ) _log.error( "onMessage:", x );
            else _log.error( "onMessage: {}", x.toString() );
        }
        finally {
            _semaphore.release();
        }
    }

    @Override
    public void onException( JMSException x )
    {
        Throwable cause = x.getCause();
        if( cause == null ) cause = x;
        _log.warn( "Connection.onException: ", cause.toString() );
    }

    private void onResponse( Response response )
    {
        _log.debug( "Received response: {}", response );
    }

    private Session _session;
    private MessageProducer _producer;
    private Connection _connection = null;
    private Logger _log = LoggerFactory.getLogger( getClass() );

    private final Semaphore _semaphore = new Semaphore( 1000 );
}
