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


require 'iudex-core'

module Iudex
  module Filters
    import 'iudex.filters.FilterChain'
    import 'iudex.filters.ListenerChain'
    import 'iudex.filters.FilterIndex'
    import 'iudex.filters.LogListener'
    import 'iudex.filters.SummaryReporter'
    import 'iudex.filters.ByFilterReporter'
    import 'iudex.core.FilterContainer'

    class FilterChainFactory
      attr_reader :description, :filters, :listeners

      def initialize( description = "default" )
        @filters = []
        @description = description

        # @lname =
        @log = SLF4J[ "iudex.filters.FilterChain.#{description}" ]

        @listeners = []

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
        log_and_register( @filters )

        create_listener_chain

        @chain = FilterChain.new( @description, @filters )

        @chain.listener = @listener

        nil
      end

      def create_listener_chain
        ll = []

        ll << LogListener.new( @log.name, @index )

        if @summary_period
          ll << SummaryReporter.new( description, @summary_period )
        end

        if @by_filter_period
          ll << ByFilterReporter.new( @index,
                                      ByFilterLogger.new( @index ),
                                      @by_filter_period )
        end

        ll += @listeners #FIXME: Or better as factory method overload?

        @listener = ListenerChain.new( ll )
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

      private

      def log_and_register( filters, depth = 0 )
        filters.each do |filter|
          name = @index.register( filter )
          @log.info { "<< " + "< " * depth + name }
          if filter.kind_of?( FilterContainer )
            log_and_register( filter.children, depth + 1 )
          end
        end
      end

    end

    class ByFilterLogger
      include ByFilterReporter::ReportWriter

      import 'com.gravitext.util.Metric'

      def initialize( index )
        @log = SLF4J[ self.class ]
        @index = index
        @nlength = index.filters.map { |f| index.name( f ).length }.max
      end

      def report( total, delta, duration_ns, counters )
        out = StringIO.new

        out << "Report total: %s ::\n" % [ fmt( total ) ]
        out << "  %-#{@nlength}s %6s %4s %6s %4s" % %w{ Filter Reject % Failed % }

        # sort counters by descending rejected + failed
        counts = counters.sort { |p,n| dropped( n[1] ) <=> dropped( p[1] ) }

        counts.each do |f,c|
          out << ( "\n  %#{@nlength}s %6s %3.0f%% %6s %3.0f%%" %
                   [ @index.name( f ),
                     fmt( c.rejected ), prc( c.rejected, total ),
                     fmt( c.failed   ), prc( c.failed, total ) ] )
        end
        @log.info( out.string )
      end

      def dropped( c )0
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
