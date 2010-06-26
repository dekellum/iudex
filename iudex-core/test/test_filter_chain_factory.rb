#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

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

require File.join( File.dirname( __FILE__ ), "setup" )

RJack::Logback.config_console( :mdc => "uhash" )
RJack::Logback[ "iudex.filter.core.FilterChain.test.reject" ].level = RJack::Logback::DEBUG

require 'iudex-core'
require 'iudex-core/filter_chain_factory'

class TestFilterChainFactory < MiniTest::Unit::TestCase
  include Iudex::Filter
  include Iudex::Core
  include Iudex::Core::Filters
  include Gravitext::HTMap

  UniMap.define_accessors

  class TestFilter < FilterBase
    def initialize
      super
      @toggle = false
    end

    def filter( c )
      @toggle = !@toggle
    end
  end

  def test_filter_chain
    fcf = FilterChainFactory.new( "test" )
    fcf.add_summary_reporter
    fcf.add_by_filter_reporter

    def fcf.feed_filters
      [ TestFilter.new ]
    end

    fcf.filter do |chain|
      content = UniMap.new
      5.times do |n|
        content.url = VisitURL.normalize( "http://foo.bar/#{n}" )
        content.type = "FEED"
        chain.filter( content )
      end
    end

    assert( ! fcf.open? )
  end

end
