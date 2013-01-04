#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2013 David Kellum
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

  import 'iudex.filter.FilterListener'

  def test_base_name
    f = FilterBase.new
    assert_equal( "i.f.FilterBase", f.name )
  end

  def test_top_level_name
    f = TestFilter.new
    assert_equal( "TestFilter", f.name )
  end

  # Filter exception may be raised from Ruby, and is handled as per
  # Java: caught by chain, forwarded to listener as failure
  def test_raise_filter_exception

    test_filter = FilterBase.new
    def test_filter.filter( map )
      raise FilterException.new( "Expected" )
    end

    listener = FilterListener.new
    class << listener
      attr_accessor :fail
      def failed( filter, map, x )
        @fail = [ filter, map, x ]
      end
    end

    fc = Core::FilterChain.new( "test", [ test_filter ] )
    fc.listener = listener

    map = UniMap.new
    refute( fc.filter( map ) )

    assert_equal( test_filter, listener.fail[0] )
    assert_equal( map,         listener.fail[1] )
    assert_instance_of( FilterException, listener.fail[2] )
  end

end
