#--
# Copyright (c) 2008-2011 David Kellum
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
require 'stringio'

module Iudex
  module Filter
    module Core

      class ByFilterLogger
        include ByFilterReporter::ReportWriter

        import 'com.gravitext.util.Metric'

        def initialize( desc, index )
          @log = RJack::SLF4J[ "iudex.filter.core.ByFilterLogger.#{desc}" ]
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
