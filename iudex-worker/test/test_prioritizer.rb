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

RJack::Logback.config_console( :stderr => true, :mdc => "uhash" )

RJack::Logback[ 'iudex' ].level = RJack::Logback::DEBUG

require 'iudex-worker'
require 'iudex-worker/prioritizer'

class TestPrioritizer < MiniTest::Unit::TestCase
  include Iudex::Worker
  include Gravitext::HTMap
  UniMap.define_accessors

  def test_identity
    m = new_map
    p = Prioritizer.new( "test", :constant => 3.2,
                         :factors => [], :impedance => 0 )

    assert( p.filter( m ) )
    assert( ( 3.2 - m.priority ).abs < 0.1  )
  end

  def new_map
    map = UniMap.new
    map.visit_start = Time.now
    map
  end
end
