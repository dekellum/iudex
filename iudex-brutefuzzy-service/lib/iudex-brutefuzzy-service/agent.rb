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

require 'iudex-brutefuzzy-service'
require 'iudex-brutefuzzy-service/destinations'

require 'hooker'

module Iudex::BruteFuzzy::Service

  class Agent
    include Iudex::Core
    include Iudex::BruteFuzzy
    include Iudex::SimHash::BruteFuzzy

    include RJack::QpidClient

    def initialize
      Hooker.apply( [:iudex,:brutefuzzy_agent], self )
    end

    def run

      service = Hooker.with( :iudex ) do |h|
        ctx = QpidJMSContext.new
        Destinations.apply( ctx )
        h.apply( :jms_context, ctx )

        h.apply( :brutefuzzy_service, Service.new( create_fuzzy_set, ctx ) )
      end

      Hooker.log_not_applied # All hooks should be used by now

      sleep #forever
    end

    def create_fuzzy_set
      FuzzyTree64.new( 500_000, 3, 16 )
    end

  end
end