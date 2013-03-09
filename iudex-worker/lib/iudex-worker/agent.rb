#--
# Copyright (c) 2008-2013 David Kellum
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

      attr_accessor :raise_on_run

      def initialize
        @log = RJack::SLF4J[ self.class ]
        @http_manager = nil
        @raise_on_run = false
        Hooker.apply( [ :iudex, :worker ], self )
      end

      def poll_keys
        [ :url, :type, :priority, :next_visit_after, :last_visit, :etag ]
      end

      # Note this can/is used to override factory in derived classes.
      def filter_chain_factory
        FilterChainFactory.new( 'agent' )
      end

      def http_client( executor )
        if defined?( JettyHTTPClient.create_client )
          @log.info "Setting up JettyHTTPClient"
          JettyHTTPClient.create_client.tap do |c|
            c.executor = executor
            c.start
          end
        elsif defined?( AsyncHTTPClient.create_client )
          @log.info "Setting up AsyncHTTPClient"
          opts = {}
          opts[ :executor_service ] = executor if executor
          AsyncHTTPClient.create_client( opts )
        else
          gem     'iudex-httpclient-3', '~> 1.2.b'
          require 'iudex-httpclient-3'
          @log.info "Setting up HTTPClient3"
          @http_manager = HTTPClient3.create_manager
          @http_manager.start
          HTTPClient3::HTTPClient3.new( @http_manager.client )
        end
      end

      def visit_manager( wpoller )
        vexec = VisitManager.new( wpoller )
        Hooker.apply( [ :iudex, :visit_manager ], vexec )
      end

      def work_poller( data_source )
        cmapper = ContentMapper.new( keys( poll_keys ) )
        wpoller = WorkPoller.new( data_source, cmapper )

        visit_q = Hooker.apply( [ :iudex, :visit_queue ], VisitQueue.new )

        wpoller.visit_queue_factory = VisitQueueFactory.new( visit_q )

        Hooker.apply( [ :iudex, :work_poller ], wpoller )
      end

      def run
        Hooker.with( :iudex ) do
          run_safe
        end
      end

      def run_safe
        dsf = PoolDataSourceFactory.new
        data_source = dsf.create

        wpoller = work_poller( data_source )
        vexec = visit_manager( wpoller )
        vexec.start_executor

        hclient = http_client( vexec.executor )

        fcf = filter_chain_factory
        fcf.http_client = hclient
        fcf.data_source = data_source
        fcf.visit_counter = vexec

        # FilterChain's executor is the same executor, unless using
        # HTTPClient3, where executor is best not used
        fcf.executor = vexec.executor unless @http_manager

        Hooker.apply( [ :iudex, :filter_factory ], fcf )

        fcf.filter do |chain|
          vexec.filter_chain = chain

          Hooker.log_not_applied # All hooks should be used by now

          vexec.start
          vexec.join # Run until interrupted
        end # fcf closes

      rescue => e
        if @raise_on_run
          raise e
        else
          @log.error( "On run: ", e )
        end
      ensure
        hclient.close if hclient && hclient.respond_to?( :close )
        @http_manager.shutdown if @http_manager
        dsf.close if dsf
      end

    end
  end
end
