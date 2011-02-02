# -*- coding: utf-8 -*-
#--
# Copyright (c) 2008-2010 David Kellum
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

module Iudex
  module Worker

    class Prioritizer < Iudex::Filter::FilterBase
      include Math

      attr_accessor :constant
      attr_accessor :impedance
      attr_accessor :min_next
      attr_accessor :min_next_unmodified
      attr_accessor :factors

      WWW_BEGINS = Time.utc( 1991, "aug", 6, 20,0,0 ) # WWW begins
      MINUTE     = 60.0
      HOUR       = 60.0 * 60.0

      def initialize( name, opts = {} )
        @name = name

        @constant            = 0.0
        @impedance           = 2.0
        @min_next_unmodified =  5 * MINUTE
        @min_next            = 10 * MINUTE

        @factors = [ [ 30.0, :ref_change_rate ],
                     [ -1.0, :log_pub_age ] ]

        @log = RJack::SLF4J[ self.class ]

        opts.each { |k,v| send( k.to_s + '=', v ) }
        yield self if block_given?

        @min_next_unmodified = [ @min_next_unmodified, @min_next ].min
        @constant = @constant.to_f
      end

      def describe
        [ @name, @constant, @min_next ]
      end

      def filter( map )

        map.priority, delta = adjust( map, map.priority )

        map.next_visit_after = ( as_time( map.visit_start ) + delta if delta )

        true
      end

      def adjust( map, priority, delta = 0.0 )

        old_priority = priority
        memo = ( ( ( @constant != 0.0 ) ? [ @constant ] : [] ) if @log.debug? )

        new_priority = @factors.inject( @constant ) do | p, (w,func)|
          comp = ( w * send( func, map ) )
          ( memo << "%.1f:%s" % [ comp.to_f, func ] ) if memo && comp != 0.0
          p + comp
        end

        #FIXME: new_priority = [ 0.0, new_priority ].max

        priority = ( ( ( priority || 0.0 ) * @impedance + new_priority ) /
                     ( @impedance + 1 ) )

        delta = ( map.status == 304 ) ? @min_next_unmodified : @min_next

        @log.debug do
          memo.join( ' + ' ) +
            ( " :: %.1f -> %.1f = %.1f in %.1fs" %
              ( [ old_priority, new_priority,
                  priority, delta ].map { |f| f.to_f } ) )
        end

        [ priority, delta ]
      end

      def log_pub_age( map )
        diff = sdiff( ( map.pub_date || WWW_BEGINS ), map.visit_start ) / MINUTE
        diff = 1.0 / MINUTE if diff < 1.0 / MINUTE
        ( log( diff ) - log( 1.0 / MINUTE ) )
      end

      # FIXME: Useful?
      # def ref_pub_age( map )
      #   map.visit_start - ( map.ref_pub_date || WWW_BEGINS )
      # end

      # References per hour, with updates rated at 1/4 a new reference.
      def ref_change_rate( map )
        s = since( map )
        if s.nil? || s == 0.0
          0.0
        else
          ( ( ( map.new_references || 0.0 ) +
              ( map.updated_references || 0.0 ) / 4.0 ) /
            s *
            HOUR )
        end
      end

      def since( map )
        sdiff( map.last_visit || oldest( map.references ),
               map.visit_start )
      end

      def oldest( refs )
        ( refs.map { |r| r.pub_date }.compact.min ) if refs
      end

      def sdiff( prev, now )
        diff = as_time( now ) - as_time( prev || WWW_BEGINS )
        ( diff < 0.0 ) ? 0.0 : diff
      end

      # FIXME: Generalize?
      def as_time( torj )
        if torj.is_a?( Time )
          torj
        else
          ms = torj.time
          Time.at( ms / 1_000, ( ms % 1_000 ) * 1_000 ) # s, µs
        end
      end

    end

  end
end
