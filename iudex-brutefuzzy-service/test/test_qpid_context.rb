#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2011 David Kellum
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

require File.join( File.dirname( __FILE__ ), "setup" )

require 'iudex-brutefuzzy-service'
require 'iudex-brutefuzzy-service/destinations'

class TestQpidContext < MiniTest::Unit::TestCase
  include RJack::QpidClient
  include Iudex::BruteFuzzy::Service

  def test_destinations
    con = nil
    ctx = QpidJMSContext.new
    Destinations.apply( ctx )

    assert( con = ctx.create_connection )
    assert( ctx.lookup_destination( 'iudex-brutefuzzy-request' ) )
    assert( ctx.lookup_destination( 'iudex-brutefuzzy-listener' ) )
  ensure
    ctx.close if ctx
    con.close if con
  end

end
