#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

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

$LOAD_PATH.unshift File.join( File.dirname(__FILE__), "..", "lib" )

require 'rubygems'

require 'rjack-logback'
Logback.config_console( :mdc => "uhash" )
Logback[ "iudex.filter.core.FilterChain.test.reject" ].level = Logback::DEBUG

require 'iudex-core'
require 'iudex-core/filter_chain_factory'

require 'test/unit'

class TestFilterChainFactory < Test::Unit::TestCase
  include Iudex::Core
  include Gravitext::HTMap

  import 'iudex.core.VisitURL'
  import 'iudex.core.ContentKeys'

  UniMap.define_accessors

  def test_filter_chain
    fcf = FilterChainFactory.new( "test" )
    fcf.add_summary_reporter( 1.0 )
    fcf.add_by_filter_reporter( 2.5 )

    fcf.filter do |chain|
      content = UniMap.new
      content.url = VisitURL.normalize( "http://foo.com/bar" )
      chain.filter( content )
    end

    assert( ! fcf.open? )
  end

end
