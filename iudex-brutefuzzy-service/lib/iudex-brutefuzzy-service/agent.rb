#--
# Copyright (c) 2011-2014 David Kellum
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

require 'iudex-brutefuzzy-service'
require 'iudex-brutefuzzy-service/destinations'

require 'hooker'

module Iudex::BruteFuzzy::Service

  class Agent
    include Iudex::Core
    include Iudex::BruteFuzzy
    include Iudex::SimHash::BruteFuzzy

    include RJack::QpidClient

    import 'rjack.jms.JMSConnector'

    def initialize
      Hooker.apply( [ :iudex, :brutefuzzy_agent ], self )
    end

    def fuzzy_set
      FuzzyTree64.new( 500_000, 3, 16 )
    end

    def jms_context
      ctx = QpidJMSContext.new
      Destinations.apply( ctx )
      ctx
    end

    def jms_connector( ctx )
      connector = JMSConnector.new( ctx )
      connector.max_connect_delay = java.lang.Integer::MAX_VALUE
      connector.do_close_connections = false
      connector
    end

    def run
      ctx = jms_context
      Hooker.apply( [ :jms, :context ], ctx )

      connector = jms_connector( ctx )
      Hooker.apply( [ :jms, :connector ], connector )

      service = Service.new( fuzzy_set )
      Hooker.apply( [ :iudex, :brutefuzzy_service ], service )

      Hooker.log_not_applied # All hooks should be used by now

      connector.add_connect_listener( service )
      connector.connect_loop
    end

  end
end
