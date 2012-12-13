#--
# Copyright (c) 2008-2012 David Kellum
#
# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License.  You
# may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.  See the License for the specific language governing
# permissions and limitations under the License.
#++

require 'iudex-http'

require 'rjack-jetty'
require 'rjack-jetty/client'

require 'hooker'

require 'iudex-jetty-httpclient/base'

require 'java'

module Iudex

  module JettyHTTPClient
    require "#{LIB_DIR}/iudex-jetty-httpclient-#{VERSION}.jar"

    import 'iudex.jettyhttpclient.Client'
    import 'iudex.jettyhttpclient.HttpClientWrapper'
    import 'org.eclipse.jetty.util.ssl.SslContextFactory'

    include RJack::Jetty

    def self.create_jetty_client( cfg = {} )

      ssl_factory = SslContextFactory.new
      ctx_cfg = cfg.delete( :ssl_context )
      ctx_cfg.each do |key,value|
        ssl_factory.__send__( "set_#{key}", value )
      end

      jclient = HttpClientWrapper.new( ssl_factory )

      cfg.each do |key,value|
        jclient.__send__( "set_#{key}", value )
      end

      jclient
    end

    def self.create_client( opts = {} )
      cfg = { :timeout                     => 6_000,
              :connect_timeout             => 3_000,
              :idle_timeout                => 6_000,
              :max_connections_per_address => 2,
              :max_queue_size_per_address  => 100,
              :dispatch_io                 => true,
              :follow_redirects            => false,
              :max_redirects               => 6,
              :ssl_context => { :trust_all => true } }

      cfg = cfg.merge( opts )
      cfg = Hooker.merge( [ :iudex, :jetty_httpclient ], cfg )

      timeout = cfg.delete( :timeout )

      client = Client.new( create_jetty_client( cfg ) )

      client.timeout = timeout if timeout

      client
    end

  end

end
