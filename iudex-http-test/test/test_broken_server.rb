#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2011 David Kellum
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

ldir = File.join( File.dirname( __FILE__ ), "..", "lib" )
$LOAD_PATH.unshift( ldir ) unless $LOAD_PATH.include?( ldir )

require 'rubygems'

require 'iudex-http-test/broken_server'
require 'net/http'
require 'thread'

require 'minitest/unit'
require 'minitest/autorun'

class TestBrokenServer < MiniTest::Unit::TestCase
  include Iudex::HTTP::Test

  def test_start_stop
    bs = BrokenServer.new
    bs.start
    bs.stop
    pass
  end

  def test_fake_http
    bs = BrokenServer.new
    bs.start
    sthread = Thread.new do
      bs.accept do |sock|
        content = "body"

        sock.write "HTTP/1.0 200 OK\r\n"
        sock.write "Content-Length: #{ content.length }\r\n"
        sock.write "Connection: close\r\n"
        sock.write "\r\n"
        sock.write content
      end
    end

    res = Net::HTTP.start( 'localhost', bs.port ) do |http|
      http.get( '/' )
    end

    sthread.join

    assert_instance_of( Net::HTTPOK, res )
  ensure
    bs.stop if bs
  end

end
