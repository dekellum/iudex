#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2015 David Kellum
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

RJack::Logback.config_console( :stderr => true, :mdc => "uhash" )

RJack::Logback[ 'iudex' ].level = RJack::Logback::DEBUG

require 'iudex-worker'
require 'iudex-worker/prioritizer'

class TestPrioritizer < MiniTest::Unit::TestCase
  include Iudex::Worker
  include Gravitext::HTMap
  JDate = Java::java.util.Date

  UniMap.define_accessors

  def test_identity
    m = new_map
    p = Prioritizer.new( "test", :constant => 3.2,
                         :factors => [], :impedance => 0 )

    assert( p.filter( m ) )
    assert_equal_fuzzy( 3.2, m.priority )
    assert_equal( m.visit_start, m.next_visit_after )
  end

  def test_visiting_now
    m = new_map
    p = Prioritizer.new( "test", :visiting_now => true )

    assert( p.filter( m ) )
    assert_equal_fuzzy( m.visit_start.time/1000.0 + p.min_next,
                        m.next_visit_after.time/1000.0 )
  end

  def test_oldest
    map = new_map

    times = [ Time.utc( 2010, "jul", 17, 19,0,0 ),
              oldest = Time.utc( 2010, "jul", 17, 18,0,0 ),
              Time.utc( 2010, "jul", 17, 20,0,0 ),
              nil ]

    map.references = times.map do |t|
      ref = UniMap.new
      ref.pub_date = t
      ref
    end

    p = prioritizer
    assert_equal( oldest, p.as_time( p.oldest( map.references ) ) )
  end

  def test_since_last
    assert_equal( 60.0, prioritizer.since( one_minute_last_map ) )
  end

  def test_ref_change_rate
    map = one_minute_last_map
    map.new_references = 1
    map.updated_references = 4
    assert_equal_fuzzy( 120, prioritizer.ref_change_rate( map ) )
  end

  def one_minute_last_map
    map = new_map
    map.visit_start = start = JDate.new
    map.last_visit = JDate.new( start.time - ( 1_000 * 60 ) )
    map
  end

  def assert_equal_fuzzy( l, r )
    assert( ( l - r ).abs < 0.1, "#{l} ~!= #{r}" )
  end

  def new_map
    map = UniMap.new
    map.visit_start = JDate.new
    map
  end

  def prioritizer
    Prioritizer.new( "test" )
  end

end
