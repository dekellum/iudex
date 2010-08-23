#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2010 David Kellum
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

$LOAD_PATH.unshift File.join( File.dirname(__FILE__), "..", "lib" )

require 'rubygems'

require 'brute-fuzzy'
require 'test/unit'

class TestFuzzySet < Test::Unit::TestCase
  include BruteFuzzy

  # Series that will allow all but last at 3 bit threshold, all at 2
  # bit threshold.
  TEST_SERIES = [ %w[ FFFF_FFFF_FFFF_FFFF
                      7FFF_7FFF_7FFF_7FFF
                      F7FF_F7FF_F7FF_F7FF
                      FF7F_FF7F_FF7F_FFFF ],

                  %w[ 0000_0000_0000_0000
                      0100_1000_1000_0010
                      1000_0100_0100_1000
                      0010_0010_0010_0001
                      0001_0001_0001_0000 ],

                  %w[ 0000_0000_0000_0000
                      0010_0100_0100_0100
                      0001_0000_1000_1001
                      0100_1001_0001_0000
                      0000_0010_0010_0010 ] ]

  def test_hex
    assert_equal( 0x1000_0000, hex( "1000_0000" ) )
    assert_equal( 0x7FFF_FFFF_FFFF_FFFF, hex( "7FFF_FFFF_FFFF_FFFF" ) )
    assert_equal( -1, hex( "FFFF_FFFF_FFFF_FFFF" ) )
  end

  def test_match
    m = FuzzyList64.new( 100, 4 )
    assert(   m.fuzzy_match( 0, 0 ) )
    assert(   m.fuzzy_match( hex( '7FFF_FFFF_FFFF_FFFF' ),
                             hex( '7FFF_FFFF_7777_FFFF' ) ) )

    assert(   m.fuzzy_match( hex( 'FFFF_FFFF_FFFF_FFFF' ),
                             hex( 'FFFF_FFFF_7777_FFFF' ) ) )

    assert( ! m.fuzzy_match( hex( '7FFF_FFFF_FFFF_FFFF' ),
                             hex( '7FFF_FFFF_EFFF_7777' ) ) )

    assert( ! m.fuzzy_match( hex( 'FFFF_FFFF_FFFF_FFFF' ),
                             hex( 'FFFF_FFFF_EFFF_7777' ) ) )
  end

  def test_add
    m = FuzzyList64.new( 100, 4 )
    assert(   m.add(  0x0 ) )
    assert(   m.add( 0xFF ) )
    assert( ! m.add( 0xFE ) )
    assert( ! m.add(  0x1 ) )
  end

  def test_series_list
    assert_series( FuzzyList64 )
  end

  def test_series_tree
    assert_series( FuzzyTree64 )
  end

  def assert_series( fclz )
    TEST_SERIES.each do |s|
      assert_series_last( fclz.new( 5, 3 ), s )
      assert_series_all(  fclz.new( 5, 2 ), s )
    end
  end

  def assert_series_last( fset, s )
    s = s.dup
    last = s.pop # Remove last for now
    assert_series_all( fset, s )
    assert( ! fset.add( hex( last ) ), last )
  end

  def assert_series_all( fset, s )
    s.each { |k| assert( fset.add( hex( k ) ), k ) }
  end

  def hex( h )
    BruteFuzzy::unsignedHexToLong( h.gsub( /_/, '' ) )
  end
end
