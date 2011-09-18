#--
# Copyright (c) 2008-2011 David Kellum
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

require 'iudex-da'
require 'iudex-da/key_helper'
require 'iudex-da/pool_data_source_factory'

require 'iudex-worker'
require 'iudex-worker/filter_chain_factory'

require 'hooker'

module Iudex
  module Worker

    class Agent
      include Iudex::DA
      include Iudex::Filter::KeyHelper
      include Iudex::Core
      include Iudex::Worker
      include Gravitext::HTMap

      attr_accessor :run_async
      attr_accessor :common_executor

      def initialize
        @log = RJack::SLF4J[ self.class ]
        @run_async = false
        @common_executor = true
        Hooker.apply( [ :iudex, :worker ], self )
      end

      def poll_keys
        [ :url, :type, :priority, :next_visit_after, :last_visit, :etag ]
      end

      # Note this can/is used to override factory in derived classes.
      def filter_chain_factory
        FilterChainFactory.new( 'agent' )
      end

      def http_client
        if @run_async
          require 'iudex-jetty-httpclient'
          @jetty_client = JettyHTTPClient.create_client
        else
          require 'iudex-httpclient-3'
          @http_3_mgr = HTTPClient3.create_manager
          @http_3_mgr.start
          HTTPClient3::HTTPClient3.new( @http_3_mgr.client )
        end
      end

      def visit_executor( wpoller )
        vexec = VisitManager.new( wpoller )
        Hooker.apply( [ :iudex, :visit_executor ], vexec )
      end

      def run
        Hooker.with( :iudex ) do
          dsf = PoolDataSourceFactory.new
          data_source = dsf.create

          cmapper = ContentMapper.new( keys( poll_keys ) )
          wpoller = WorkPoller.new( data_source, cmapper )
          Hooker.apply( :work_poller, wpoller )

          hclient = http_client

          fcf = filter_chain_factory
          fcf.http_client = hclient
          fcf.data_source = data_source

          Hooker.apply( :filter_factory, fcf )

          vexec = visit_executor( wpoller )
          fcf.visit_counter = vexec

          fcf.filter do |chain|
            vexec.filter_chain = chain
            if @run_async
              hclient.executor = vexec.start_executor if @common_executor
              hclient.start
            end

            Hooker.log_not_applied # All hooks should be used by now

            vexec.start
            vexec.join    #Run until interrupted
          end # fcf closes

          @http_3_mgr.shutdown if @http_3_mgr
          @jetty_client.close if @jetty_client
          dsf.close
        end
      rescue => e
        @log.error( "On run: ", e )
      end

    end

  end
end
