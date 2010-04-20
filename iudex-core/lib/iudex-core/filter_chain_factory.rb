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
    class FilterChainFactory < Iudex::Filter::Core::FilterChainFactory
      import 'iudex.filter.core.MDCUnsetter'
      import 'iudex.filter.core.Switch'
      import 'iudex.filter.core.Selector'

      import 'iudex.core.ContentKeys'
      import 'iudex.core.filters.UHashMDCSetter'
      import 'iudex.core.filters.ContentFetcher'
      import 'iudex.core.filters.TextCtrlWSFilter'

      def filters
        [ UHashMDCSetter.new ] + super + [ type_switch ]
      end

      def listeners
        super + [ MDCUnsetter.new( "uhash" ) ]
      end

      def type_map
        { "FEED" => :feed_filters,
          "PAGE" => :page_filters }
      end

      def type_switch
        switch = Switch.new
        type_map.each do |t,ff|
          create_chain( t.downcase, send( ff ) ) do |c|
            switch.add_proposition( Selector.new( ContentKeys::TYPE, t ), c )
          end
        end
        switch
      end

      def feed_filters
      end

      def page_filters
      end

    end
  end
end
