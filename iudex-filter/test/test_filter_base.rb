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

require 'gravitext-util'
require 'iudex-filter/proc_filter'

class TestFilter < Iudex::Filter::FilterBase
end

class TestFilterBase < MiniTest::Unit::TestCase
  include Iudex::Filter
  include Gravitext::HTMap

  def test_base_name
    f = FilterBase.new
    assert_equal( "i.f.FilterBase", f.name )
  end

  def test_top_level_name
    f = TestFilter.new
    assert_equal( "TestFilter", f.name )
  end

end
