/*
 * Copyright (c) 2011-2015 David Kellum
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
import iudex.brutefuzzy.protobuf.ProtocolBuffers.Response;
import iudex.filter.core.PeriodicNotifier;
import iudex.simhash.brutefuzzy.FuzzySet64;

import java.util.TreeSet;

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

import rjack.jms.ConnectListener;
import rjack.jms.JMSContext;

import com.google.protobuf.InvalidProtocolBufferException;
import com.gravitext.util.Metric;

public class Service
    implements MessageListener, ConnectListener
{
    public Service( FuzzySet64 fuzzySet )
    {
        _fuzzySet = fuzzySet;
    }

    @Override
    public void onConnect( JMSContext context, Connection connection )
        throws JMSException, NamingException
    {
        _session = context.createSession( connection );

        Destination requestQueue =
            context.lookupDestination( "brutefuzzy-request" );

        Destination responseDest =
            context.lookupDestination( "brutefuzzy-response-ex" );

        _producer = _session.createProducer( responseDest );

        MessageConsumer consumer = _session.createConsumer( requestQueue );
        consumer.setMessageListener( this );
    }

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
            _notifier.tick();
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
            if( ! _fuzzySet.addFindAll( simhash, matches ) ) ++_count;
            _found += matches.size();
            if( _sendNoMatchResponse || ( matches.size() > 0 ) ) {
                sendResponse( msg, simhash, matches );
            }
            break;
        }

        case CHECK_ONLY: {
            TreeSet<Long> matches = new TreeSet<Long>();
            _fuzzySet.findAll( simhash, matches );
            _found += matches.size();
            if( _sendNoMatchResponse || ( matches.size() > 0 ) ) {
                sendResponse( msg, simhash, matches );
            }
            break;
        }

        case REMOVE:
            if( _fuzzySet.remove( simhash ) ) --_count;
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

        _producer.send( response );

        _log.debug( "Sent response: {}", response );
    }

    class Notifier extends PeriodicNotifier
    {
        Notifier()
        {
            super( 10.0d ); //seconds
        }

        @Override
        protected void notify( long total, long tDelta, long duration )
        {
            long cDelta = _count - _lastCount;
            long fDelta = _found - _lastFound;

            double tRate = ( (double) tDelta ) / duration * 1e9d;
            double cRate = ( (double) cDelta ) / duration * 1e9d;
            double fRate = ( (double) fDelta ) / duration * 1e9d;

            double cRatio = ( (double) _count ) / total * 100.0d;

            _lastFound = _found;
            _lastCount = _count;

            _log.info( String.format(
                "Q: %s %s/s C: %s %3.0f%% %s/s F: %s %s/s ",
                Metric.format( total ),
                Metric.format( tRate ),
                Metric.format( _count ),
                cRatio,
                Metric.format( cRate ),
                Metric.format( _found ),
                Metric.format( fRate ) ) );
        }
    }

    private final FuzzySet64 _fuzzySet;
    private final boolean _sendNoMatchResponse = false;
    private final Logger _log = LoggerFactory.getLogger( getClass() );
    private Session _session = null;
    private MessageProducer _producer;

    private final Notifier _notifier = new Notifier();
    private long _count = 0;
    private long _lastCount = 0;
    private long _found = 0;
    private long _lastFound = 0;
}
