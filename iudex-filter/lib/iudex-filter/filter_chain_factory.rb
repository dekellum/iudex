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

require 'iudex-filter'
require 'iudex-filter/key_helper'
require 'iudex-filter/by_filter_logger'

module Iudex
  module Filter
    module Core

      class FilterChainFactory
        attr_reader :description

        attr_accessor :main_summary_period
        attr_accessor :main_by_filter_period

        include KeyHelper

        def initialize( description = "default" )
          @description = description

          @log = RJack::SLF4J[ [ RJack::SLF4J.to_log_name( self.class ),
                                 description ].join('.') ]

          @main_summary_period = 10.0
          @main_by_filter_period = 60.0

          @index = nil
          @chain = nil
          @listener = nil
        end

        # Deprecated: Use main_summary_period accessor
        def add_summary_reporter( period_s = 10.0 )
          @main_summary_period = period_s
        end

        # Deprecated: Use main_by_filter_period accessor
        def add_by_filter_reporter( period_s = 60 * 10.0 )
          @main_by_filter_period = period_s
        end

        def open
          close if open?

          @index = FilterIndex.new

          # Temp setup of empty listener, since full listeners setup
          # requires filters, log_and_register which itself requires
          # listeners via create_chain
          @listener = place_holder = NoOpListener.new

          flts = filters.flatten.compact
          log_and_register( flts )

          @chain = create_chain( :desc     => @description,
                                 :filters  => flts,
                                 :listener => :main )
          @listener = ListenerChain.new( listeners )

          # Now replace the temp listener with the final listener
          # chain
          replace_listeners( @chain, place_holder, @listener )

          # With all filters loaded and thus key references, make sure
          # UniMap accessors are defined (for ruby filters)
          Gravitext::HTMap::UniMap.define_accessors

          nil
        end

        def open?
          @chain != nil
        end

        def close
          if @chain
            @chain.close
            @chain = nil
          end

          if @listener
            @listener.close
            @listener = nil
          end
        end

        # Yields chain to block, bounded by open/close if not already open
        def filter
          opened = unless open?
                     open
                     true
                   end

          yield @chain

        ensure
          close if opened
        end

        def filters
          []
        end

        def listeners
          create_listeners( @description,
                            @main_summary_period,
                            @main_by_filter_period )
        end

        # Create, yield to optional block, and returns FilterChain if
        # provided filters is not empty. If empty returns a NoOpFilter
        # and but does not yield.
        #
        # === Options
        #
        # A trailing Hash argument is interpreted as options with the
        # following keys. Alternative positional parameters equivalent
        # to ( :desc, :filters = nil, :listener = nil ) are
        # deprecated.
        #
        # :desc:: ~>to_s Description of this chain.
        # :filters:: Symbol method name to send for filters (and also
        #            default description), or an Array of filters.
        # :listener:: The :main symbol for this filter chains
        #             listeners, or an alternative FilterListener
        #             instance.
        # :pass:: If truthy, the chain always returns true (pass) on
        #         filter.
        #
        def create_chain( *args )
          # ( desc, flts = nil, listener = nil )

          opts = args.last.is_a?( Hash ) ? args.pop.dup : {}
          opts[ :listener ] ||= args[2] if args.length > 2
          opts[ :filters ]  ||= args[1] if args.length > 1

          if args[ 0 ].is_a?( Symbol )
            opts[ :desc ] = args[ 0 ].to_s.gsub( /_/, '-' )
            opts[ :filters ] = args[ 0 ]
          else
            opts[ :desc ] ||= args[ 0 ]
          end

          if opts[ :filters ].is_a?( Symbol )
            opts[ :filters ] = send( opts[ :filters ] )
          end

          flts = Array( opts[ :filters ] ).flatten.compact

          if flts.empty?
            NoOpFilter.new
          else
            c = FilterChain.new( opts[ :desc ], flts )

            if opts[ :listener ] == :main
              c.listener = @listener
            elsif opts[ :listener ].nil?
              c.listener = log_listener( opts[ :desc ] )
            else
              c.listener = opts[ :listener ]
            end

            c.always_pass = true if opts[ :pass ]

            yield c if block_given?
            c
          end
        end

        # Create a new Switch given selector key and map of values to
        # filters, or values to [ filters, listener ]
        def create_switch( key, value_filters_map )
          switch = Switch.new
          value_filters_map.each do |value, (filters, listener)|
            create_chain( :desc     => value.to_s.downcase,
                          :filters  => filters,
                          :listener => listener ) do |chain|
              switch.add_proposition( Selector.new( key, value ), chain )
            end
          end
          switch
        end

        private

        def create_listeners( desc, summary_period, by_filter_period )
          [ log_listener( desc ),
            ( SummaryReporter.new( desc, summary_period ) if summary_period ),
            ( ByFilterReporter.new( @index,
                                    ByFilterLogger.new( desc, @index ),
                                    by_filter_period ) if by_filter_period )
          ].compact
        end

        def log_listener( desc )
          LogListener.new( "iudex.filter.core.FilterChain.#{desc}", @index )
        end

        def log_and_register( filters, depth = 0 )
          filters.each do |filter|
            name = @index.register( filter )
            @log.info { "<< " + "  " * depth + name }
            if filter.kind_of?( FilterContainer )
              log_and_register( filter.children, depth + 1 )
            end
          end
        end

        def replace_listeners( filter, place_holder, listener )
          if filter.kind_of?( FilterContainer )
            if filter.kind_of?( FilterChain )
              if filter.listener == place_holder
                filter.listener = listener
              end
            end
            filter.children.each do |c|
              replace_listeners( c, place_holder, listener )
            end
          end
        end

      end

    end
  end
end
