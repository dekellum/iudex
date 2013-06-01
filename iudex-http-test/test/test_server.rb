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

require 'iudex-http-test/helper'
require 'net/http'

class TestServer < MiniTest::Unit::TestCase
  include Iudex::HTTP::Test
  include Helper
  CustomUnit.register

  def test_byte_array_new
    # Fishwife uses this.
    # Seeing NPE on:
    # jruby 1.5.6 (ruby 1.8.7 patchlevel 249) (2010-12-03 9cf97c3)
    #             (OpenJDK Client VM 1.6.0_20) [i386-java]
    #             (Java HotSpot(TM) Client VM 1.6.0_26) [i386-java]
    #             (Java HotSpot(TM) Client VM 1.7.0) [i386-java]
    # But not jruby 1.6.2, or either with Server VM
    Java::byte[4096].new
    pass
  end

  def test_port
    assert server.port > 0
  end

  def test_index
    res = Net::HTTP.start( 'localhost', server.port ) do |http|
      http.get( '/index' )
    end
    assert_instance_of( Net::HTTPOK, res )
  end

end
