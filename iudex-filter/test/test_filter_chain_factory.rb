#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

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

require File.join( File.dirname( __FILE__ ), "setup" )

RJack::Logback.config_console( :stderr => true, :mdc => "tkey" )

# RJack::Logback[ "iudex.filter.core.FilterChain.test.reject" ].level = RJack::Logback::DEBUG

require 'gravitext-util'
require 'iudex-filter/filter_chain_factory'

class TestFilterChainFactory < MiniTest::Unit::TestCase
  include Iudex::Filter
  include Iudex::Filter::Core

  include Gravitext::HTMap

  import 'iudex.filter.core.MDCSetter'
  import 'iudex.filter.core.MDCUnsetter'

  TKEY = UniMap.create_key( 'tkey' );

  class RandomFilter < FilterBase

    def initialize( odds = 2 )
      @odds = odds
    end

    def describe
      [ @odds ]
    end

    def filter( map )
      rand( @odds ) != 0
    end
  end

  def test_filter_chain
    fcf = FilterChainFactory.new( "test" )
    fcf.main_summary_period = 1.0
    fcf.main_by_filter_period = 2.5

    def fcf.filters
      [ MDCSetter.new( TKEY ) ] + super +
        [ 6, 4, 6, 6 ].map { |p| RandomFilter.new( p ) }
    end

    def fcf.listeners
      super + [ MDCUnsetter.new( TKEY ) ]
    end

    2.times do |r|
      refute( fcf.open? )

      fcf.filter do |chain|
        1000.times do |t|
          sleep( rand( 10 ) / 1000.0 / ( r + 1 ) )
          map = UniMap.new
          map.tkey = t
          chain.filter( map )
        end
      end

      refute( fcf.open? )
    end

  end

  def test_nested_reporting
    fcf = FilterChainFactory.new( "test" )
    class << fcf
      attr_accessor :summary_reporter
      def filters
        [ create_chain( :sub_filters, nil, :main ) ]
      end
      def listeners
        super.tap do |ll|
          @summary_reporter = ll[1] #FIXME: Brittle
        end
      end

      def sub_filters
        [ 6, 4, 6, 6 ].map { |p| RandomFilter.new( p ) }
      end
    end

    fcf.filter do |chain|
      100.times do |t|
        map = UniMap.new
        map.tkey = t
        chain.filter( map )
      end
    end

    assert_equal( 100, fcf.summary_reporter.total_count )

  end

end
