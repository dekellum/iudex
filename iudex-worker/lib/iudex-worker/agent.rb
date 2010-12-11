#--
# Copyright (c) 2008-2010 David Kellum
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

require 'iudex-httpclient-3'

require 'iudex-worker'
require 'iudex-worker/filter_chain_factory'

module Iudex
  module Worker

    class Agent
      include Iudex::DA
      include Iudex::Filter::KeyHelper
      include Iudex::Core
      include Iudex::HTTPClient3
      include Iudex::Worker
      include Gravitext::HTMap

      def initialize
        Config.do_agent( self )
      end

      def poll_keys
        [ :url, :type, :priority, :next_visit_after, :last_visit, :etag ]
      end

      def run
        dsf = PoolDataSourceFactory.new
        data_source = dsf.create

        cmapper = ContentMapper.new( keys( poll_keys ) )

        wpoller = WorkPoller.new( data_source, cmapper )
        Config.do_work_poller( wpoller )

        mgr = http_client_3_manager
        mgr.start
        http_client = Iudex::HTTPClient3::HTTPClient3.new( mgr.client )

        fcf = filter_chain_factory( http_client, data_source )

        Config.do_filter_factory( fcf )

        fcf.filter do |chain|
          vexec = VisitExecutor.new( chain, wpoller )
          Config.do_visit_executor( vexec )
          vexec.start
          vexec.join    #Run until interrupted
        end # fcf closes

        mgr.shutdown
        dsf.close

      end

      def http_client_3_manager
        Config.do_http_client_3
      end

      def filter_chain_factory( http_client, data_source )
        fcf = FilterChainFactory.new( 'agent' )
        fcf.http_client = http_client
        fcf.data_source = data_source
        fcf
      end

    end

  end
end
