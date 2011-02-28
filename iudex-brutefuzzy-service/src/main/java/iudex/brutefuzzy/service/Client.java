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

import iudex.brutefuzzy.protobuf.ProtocolBuffers.Request;
import iudex.brutefuzzy.protobuf.ProtocolBuffers.Request.Builder;
import iudex.brutefuzzy.protobuf.ProtocolBuffers.RequestAction;
import iudex.brutefuzzy.protobuf.ProtocolBuffers.Response;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rjack.jms.JMSConnector;
import rjack.jms.JMSContext;
import rjack.jms.SessionExecutor;
import rjack.jms.SessionState;
import rjack.jms.SessionStateFactory;
import rjack.jms.SessionTask;

import com.google.protobuf.InvalidProtocolBufferException;

public class Client implements SessionStateFactory<Client.State>
{
    public Client( JMSConnector connector )
    {
        _executor = new SessionExecutor<State>( connector, this,
                                                10000, 1, 80 * 1000 );
    }

    public void check( long simhash, boolean doAdd )
        throws JMSException, NamingException, InterruptedException
    {
        _executor.execute( new CheckTask( simhash, doAdd ) );
    }

    public void close()
    {
        _executor.shutdown();
        try {
            _executor.awaitTermination( 10, TimeUnit.SECONDS );
        }
        catch( InterruptedException e ) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public State createSessionState( JMSContext context, Connection connection )
        throws JMSException, NamingException
    {
        return new State( context, connection );
    }

    class CheckTask extends SessionTask<State>
    {
        CheckTask( long simhash, boolean doAdd )
        {
            _simhash = simhash;
            _doAdd = doAdd;
        }

        @Override
        public void runTask( State state ) throws JMSException
        {
            try {
                state.check( _simhash, _doAdd );
            }
            catch( InterruptedException e ) {
                Thread.currentThread().interrupt();
            }
        }

        private long _simhash;
        private boolean _doAdd;
    }

    class State extends SessionState
        implements MessageListener
    {
        public State( JMSContext context, Connection connection )
            throws JMSException, NamingException
        {
            super( context, connection );

            Destination requestQ =
                context.lookupDestination( "iudex-brutefuzzy-request" );
            _producer = session().createProducer( requestQ );

            Destination responseQ =
                context.lookupDestination( "iudex-brutefuzzy-listener" );

            session().createConsumer( responseQ ).setMessageListener( this );
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

        private void onResponse( Response response )
        {
            _log.debug( "Received response: {}", response );
        }

        public void check( long simhash, boolean doAdd )
            throws JMSException, InterruptedException
        {
            Builder bldr = Request.newBuilder();
            bldr.setSimhash( simhash );
            bldr.setAction( doAdd ? RequestAction.ADD :
                                    RequestAction.CHECK_ONLY );

            BytesMessage response = session().createBytesMessage();
            response.writeBytes( bldr.build().toByteArray() );

            //FIXME: Replace with proper flow control?
            _semaphore.tryAcquire( 1, TimeUnit.SECONDS );

            _producer.send( response );
        }

        private final MessageProducer _producer;
        private final Semaphore _semaphore = new Semaphore( 10000 );
    }

    private final SessionExecutor<State> _executor;
    private final Logger _log = LoggerFactory.getLogger( getClass() );
}
