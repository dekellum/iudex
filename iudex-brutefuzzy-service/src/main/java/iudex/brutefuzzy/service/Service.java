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

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.naming.NamingException;

import com.google.protobuf.InvalidProtocolBufferException;

import iudex.jms.JMSContext;

public class Service implements MessageListener
{
    public Service( JMSContext context ) throws JMSException, NamingException
    {
        _context = context;

        Connection connection = _context.createConnection();

        Session session = _context.createSession( connection );

        Destination requestQueue =
            _context.lookupDestination( "iudex-brutefuzzy-service" );

        //FIXME: Useful here? _context.close()

        MessageConsumer consumer = session.createConsumer( requestQueue );
        consumer.setMessageListener( this );
        connection.start();
     }

    @Override
    public void onMessage( Message msg )
    {
        try {
            if( msg instanceof BytesMessage ) {
                BytesMessage bmsg = (BytesMessage) msg;
                byte[] body = new byte[ (int) bmsg.getBodyLength() ];
                bmsg.readBytes( body );
                onRequest( Request.parseFrom( body ) );
            }
        }
        catch( JMSException x ) {
            //FIXME:
        }
        catch( InvalidProtocolBufferException e ) {
            //FIXME:
        }
    }

    private void onRequest( Request parseFrom )
    {
        // TODO Auto-generated method stub
    }

    private JMSContext _context;
}
