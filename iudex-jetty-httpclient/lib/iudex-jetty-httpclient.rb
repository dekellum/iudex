#--
# Copyright (c) 2011 David Kellum
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

    include RJack::Jetty

    def self.create_jetty_client( opts = {} )
      cfg = { :timeout                     => 6_000,
              :so_timeout                  => 5_000,
              :connect_timeout             => 3_000,
              :idle_timeout                => 6_000,
              :max_retries                 => 1,
              :max_redirects               => 6,
              :max_connections_per_address => 2,
              :max_queue_size_per_address  => 100,
              :connect_blocking            => false,
              :handle_redirects_internal   => true }

      cfg = cfg.merge( opts )
      cfg = Hooker.merge( [ :iudex, :jetty_httpclient ], cfg )

      jclient = HttpClient.new

      redir_listen = cfg.delete( :handle_redirects_internal )

      cfg.each do |key,value|
        jclient.__send__( "set_#{key}", value )
      end

      if redir_listen
        jclient.register_listener( 'iudex.jettyhttpclient.RedirectListener' )
      end

      jclient
    end

    def self.create_client( opts = {} )
      Client.new( create_jetty_client( opts ) )
    end

  end

end
