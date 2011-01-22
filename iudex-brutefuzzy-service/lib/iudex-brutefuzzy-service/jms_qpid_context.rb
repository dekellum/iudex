#--
# Copyright (c) 2011 David Kellum
#
# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License.  You may
# obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.  See the License for the specific language governing
# permissions and limitations under the License.
#++

require 'rjack-qpid-client'

module Iudex

  class JMSQpidContext
    include Java::iudex.jms.JMSContext

    import 'java.util.Properties'
    import 'javax.naming.Context'
    import 'javax.naming.InitialContext'
    import 'javax.jms.Session'

    attr_accessor :username  #Required non-nil
    attr_accessor :password  #Required non-nil
    attr_accessor :brokers
    attr_accessor :virtual_host
    attr_accessor :client_id
    attr_accessor :factory_jndi_name
    attr_accessor :session_acknowledge_mode
    attr_accessor :destinations

    def initialize
      @username = 'qpid'
      @password = 'password'
      @brokers = [ [ "localhost" ] ]

      @virtual_host = 'default-vhost'
      @client_id    = 'default-client'

      @factory_jndi_name = 'local'

      @session_acknowledge_mode = Session::AUTO_ACKNOWLEDGE
      @destinations = {}
    end

    def context
      @context ||= InitialContext.new( properties )
    end

    def connection_factory
      @con_factory ||= context.lookup( factory_jndi_name )
    end

    def create_connection
      connection_factory.create_connection
    end

    def create_session( connection )
      connection.create_session( false, session_acknowledge_mode )
    end

    def lookup_destination( name )
      context.lookup( name )
    end

    def close
      @con_factory = nil

      @context.close if @context
      @context = nil
    end

    # 3.2. Apache Qpid JNDI Properties for AMQP Messaging
    # http://qpid.apache.org/books/0.8/Programming-In-Apache-Qpid/html/ch03s02.html
    def properties
      props = Properties.new

      props.put( Context.INITIAL_CONTEXT_FACTORY,
                 "org.apache.qpid.jndi.PropertiesFileInitialContextFactory" )

      props.put( [ "connectionfactory", factory_jndi_name ].join( '.' ),
                 connection_url )

      @destinations.each_pair do |name,opts|
        opts = opts.dup
        subject = opts.delete( :subject )
        props.put( [ "destination", name ].join( '.' ),
                   address_serialize( name, subject, opts ) )
      end

      props
    end

    # Serialize destination addresses (new format)
    # http://qpid.apache.org/books/0.8/Programming-In-Apache-Qpid/html/ch02s04.html
    # Reference: section 2.3.4.5
    def address_serialize( address, subject = nil, opts = nil )
      out = address.to_s
      out += '/' + subject.to_s if subject
      out += '; ' + option_serialize( opts ) if opts
    end

    def option_serialize( obj )
      if obj.is_a?( Symbol ) || obj.kind_of?( Integer )
        obj.to_s
      elsif obj.is_a?( String )
        obj.to_s.inspect #quote/escape
      elsif obj.respond_to?( :each_pair ) #Hash
        pairs = obj.map do | key, value |
          [ wrap_key( key ), option_serialize( value ) ].join( ": " )
        end
        '{ ' + pairs.join( ', ' ) + ' }'
      else
        values = obj.map do | value |
          option_serialize( value )
        end
        '[ ' + values.join( ', ' ) + ' ]'
      end
    end

    def wrap_key( key )
      k = key.to_s
      if ( k =~ /^[a-zA-Z_][a-zA-Z0-9_-]*[a-zA-Z0-9_]$/ )
        k
      else
        "'" + k + "'"
      end
    end

    def connection_url
      url = "amqp://"

      if username
        url += username
        url += ':' + password if password
        url += '@'
      end

      url += [ client_id, virtual_host ].join( '/' )

      url += '?'

      url += "brokerlist='#{ broker_list }'"

      url
    end

    def broker_list
      l = brokers.map do | host, port |
        'tcp://%s:%s' % [ host, port || 5672 ]
      end

      l.join( ';' )
    end

  end
end
