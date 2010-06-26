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

require 'iudex-filter'
require 'iudex-filter/filter_chain_factory'

require 'iudex-core'

module Iudex
  module Core
    module Filters

      class FilterChainFactory < Iudex::Filter::Core::FilterChainFactory
        include Iudex::Filter::Core

        def filters
          [ UHashMDCSetter.new ] + super + [ type_switch ]
        end

        def listeners
          super + [ MDCUnsetter.new( "uhash" ) ]
        end

        def type_map
          { "FEED" => feed_filters,
            "PAGE" => page_filters }
        end

        def type_switch( tmap = type_map )
          create_switch( ContentKeys::TYPE, tmap )
        end

        def feed_filters
        end

        def page_filters
        end

      end
    end
  end
end
