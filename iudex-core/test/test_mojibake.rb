#!/usr/bin/env jruby
# -*- coding: utf-8 -*-
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2014 David Kellum
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
require 'iudex-core/mojibake'

class TestMojiBake < MiniTest::Unit::TestCase
  include Gravitext::HTMap
  include Iudex::Core
  include Iudex::Core::Filters

  UniMap.define_accessors

  FILTER = MojiBakeFilter.new( ContentKeys::SUMMARY )

  def test_nomatch_recover
    assert_filter( '', '' )
    assert_filter( 'ascii', 'ascii' )
    assert_filter( 'Â', 'Â' )
  end

  def test_simple_recover
    assert_filter( '[°]', '[Â°]' )
    assert_filter( '“quoted”', 'â€œquotedâ€�' )
    assert_filter( '“quoted”', 'âquotedâ€' )
  end

  def test_recursive_recover
    assert_filter( '°', 'Ã‚°' )
    assert_filter( 'AP – Greenlake', 'AP Ã¢â‚¬â€œ Greenlake' )
    assert_filter( 'you’re', 'youÃ¢â‚¬â„¢re' )
  end

  def assert_filter( output, input )
    map = UniMap.new
    map.summary = input
    assert( FILTER.filter( map ) )
    assert_equal( output, map.summary.to_s, "From: #{input}" )
  end

end
