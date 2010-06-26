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
require 'iudex-filter/by_filter_logger'

module Iudex
  module Filter
    module Core

      class FilterChainFactory

        attr_reader :description

        def initialize( description = "default" )
          @description = description

          @log = RJack::SLF4J[ [ RJack::SLF4J.to_log_name( self.class ),
                                 description ].join('.') ]

          @summery_period = nil
          @by_filter_period = nil

          @index = nil
          @chain = nil
          @listener = nil
        end

        def add_summary_reporter( period_s = 10.0 )
          @summary_period = period_s
        end

        def add_by_filter_reporter( period_s = 60 * 10.0 )
          @by_filter_period = period_s
        end

        def open
          close if open?

          @index = FilterIndex.new

          flts = filters
          log_and_register( flts )

          @listener = ListenerChain.new( listeners )
          @chain = create_chain( @description, flts )
          @chain.listener = @listener

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

        def log_listener( desc )
          LogListener.new( "iudex.filter.core.FilterChain.#{desc}", @index )
        end

        def listeners
          ll = [ log_listener( @description ) ]

          if @summary_period
            ll << SummaryReporter.new( @description, @summary_period )
          end

          if @by_filter_period
            ll << ByFilterReporter.new( @index,
                                        ByFilterLogger.new( @description, @index ),
                                        @by_filter_period )
          end
          ll
        end

        def create_chain( desc, flts )
          unless flts.nil? || flts.empty?
            c = FilterChain.new( desc, flts )
            c.listener = log_listener( desc )
            yield c if block_given?
            c
          end
        end

        private

        def log_and_register( filters, depth = 0 )
          filters.each do |filter|
            name = @index.register( filter )
            @log.info { "<< " + "  " * depth + name }
            if filter.kind_of?( FilterContainer )
              log_and_register( filter.children, depth + 1 )
            end
          end
        end
      end

    end
  end
end
