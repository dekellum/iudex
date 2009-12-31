#--
# Copyright (C) 2008-2009 David Kellum
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

module Iudex
  module Filter
    import 'iudex.filter.FilterContainer'

    module Core
      import 'iudex.filter.core.ByFilterReporter'

      class FilterChainFactory
        import 'iudex.filter.core.FilterChain'
        import 'iudex.filter.core.ListenerChain'
        import 'iudex.filter.core.FilterIndex'
        import 'iudex.filter.core.LogListener'
        import 'iudex.filter.core.SummaryReporter'

        attr_reader :description

        def initialize( description = "default" )
          @description = description

          @log = SLF4J[ "iudex.filter.core.FilterChain.#{description}" ]

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
          @chain = FilterChain.new( @description, flts )

          @chain.listener = @listener = ListenerChain.new( listeners )

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
          ll = [ LogListener.new( @log.name, @index ) ]

          if @summary_period
            ll << SummaryReporter.new( description, @summary_period )
          end

          if @by_filter_period
            ll << ByFilterReporter.new( @index,
                                        ByFilterLogger.new( @description, @index ),
                                        @by_filter_period )
          end
          ll
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

      class ByFilterLogger
        include ByFilterReporter::ReportWriter

        import 'com.gravitext.util.Metric'

        def initialize( desc, index )
          @log = SLF4J[ "iudex.filter.core.ByFilterLogger.#{desc}" ]
          @index = index
          @nlength = index.filters.map { |f| index.name( f ).length }.max
        end

        def report( total, delta, duration_ns, counters )
          out = StringIO.new

          out << "Report total: %s ::\n" % [ fmt( total ) ]
          out << ( "  %-#{@nlength}s %6s %5s %6s %6s" %
                   %w{ Filter Accept % Reject Failed } )

          accepted = total
          @index.filters.each do |f|
            c = counters[ f ]
            d = dropped( c )
            if d > 0
              p = prc( -d, accepted )
              accepted -= d
              out << ( "\n  %-#{@nlength}s %6s %4.0f%% %6s %6s" %
                       [ @index.name( f ),
                         fmt( accepted ), p,
                         fmt( c.rejected ), fmt( c.failed ) ] )
            end
          end
          @log.info( out.string )
        end

        def dropped( c )
          c.rejected + c.failed
        end

        def fmt( v )
          Metric::format( v )
        end

        def prc( v, t )
          ( t > 0 ) ? v.to_f / t * 100.0 : 0.0
        end

      end
    end

  end
end
