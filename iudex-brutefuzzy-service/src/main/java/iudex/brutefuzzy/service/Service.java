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

import java.util.TreeSet;

import iudex.brutefuzzy.protobuf.ProtocolBuffers.Request;
import iudex.brutefuzzy.protobuf.ProtocolBuffers.Response;

import iudex.simhash.brutefuzzy.FuzzySet64;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import iudex.jms.JMSContext;

public class Service implements MessageListener
{
    public Service( FuzzySet64 fuzzySet, JMSContext context )
        throws JMSException, NamingException
    {
        _fuzzySet = fuzzySet;

        Connection connection = context.createConnection();

        _session = context.createSession( connection );

        _producer = _session.createProducer( null ); //destination from request.

        Destination requestQueue =
            context.lookupDestination( "iudex-brutefuzzy-request" );

        context.close();

        MessageConsumer consumer = _session.createConsumer( requestQueue );
        consumer.setMessageListener( this );

        connection.start();
     }

    //FIXME: Add open/close methods?

    @Override
    public void onMessage( Message msg )
    {
        try {
            if( msg instanceof BytesMessage ) {
                BytesMessage bmsg = (BytesMessage) msg;
                byte[] body = new byte[ (int) bmsg.getBodyLength() ];
                bmsg.readBytes( body );
                onRequest( bmsg, Request.parseFrom( body ) );
            }
            else {
                _log.error( "Received invalid message type: {}",
                            msg.getClass().getName() );
            }
            msg.acknowledge();
        }
        catch( JMSException x ) {
            if( _log.isDebugEnabled() ) _log.error( "onMessage:", x );
            else _log.error( "onMessage: {}", x.toString() );
        }
        catch( InvalidProtocolBufferException x ) {
            if( _log.isDebugEnabled() ) _log.error( "onMessage:", x );
            else _log.error( "onMessage: {}", x.toString() );
        }
    }

    private void onRequest( BytesMessage msg, Request request )
        throws JMSException
    {
        _log.debug( "Received request: {}", request );

        final long simhash = request.getSimhash();

        switch( request.getAction() ) {
        case ADD: {
            TreeSet<Long> matches = new TreeSet<Long>();
            _fuzzySet.addFindAll( simhash, matches );
            sendResponse( msg, simhash, matches );
            break;
        }

        case CHECK_ONLY: {
            TreeSet<Long> matches = new TreeSet<Long>();
            _fuzzySet.findAll( simhash, matches );
            sendResponse( msg, simhash, matches );
            break;
        }

        case REMOVE:
            _fuzzySet.remove( simhash );
            break;

        default:
            _log.warn( "Dropping unknown request action, number: {}",
                       request.getAction().getNumber() );
        }
    }

    private void sendResponse( BytesMessage msg,
                               long simhash,
                               TreeSet<Long> matches )
        throws JMSException
    {
        BytesMessage response = _session.createBytesMessage();
        response.setJMSCorrelationID( msg.getJMSMessageID() );

        Response.Builder builder = Response.newBuilder();
        builder.setSimhash( simhash );
        builder.addAllMatches( matches );

        response.writeBytes( builder.build().toByteArray() );

        Destination destination = msg.getJMSReplyTo();

        _producer.send( destination, response );

        _log.debug( "Sent response: {}", response );
    }

    private final FuzzySet64 _fuzzySet;
    private final Logger _log = LoggerFactory.getLogger( getClass() );
    private Session _session = null;
    private MessageProducer _producer;
}
