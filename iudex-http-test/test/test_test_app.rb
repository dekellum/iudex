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

require File.join( File.dirname( __FILE__ ), "setup" )

require 'rack/test'

require 'iudex-http-test/test_app'

class TestTestApp < MiniTest::Unit::TestCase
  include Iudex::HTTP::Test
  include Rack::Test::Methods

  def app
    TestApp.new
  end

  def test_root
    get '/'
    assert_equal( 302, last_response.status )
  end

  def test_301
    get '/301'
    assert_equal( 301, last_response.status )
  end

  def test_redirects
    get '/redirects/multi/2'
    assert_equal( 302, last_response.status )
  end

  def test_redirects_code
    get '/redirects/multi/2?code=301'
    assert_equal( 301, last_response.status )
  end

  def test_redirects_complete
    get '/redirects/multi/1'
    assert_equal( 200, last_response.status )
  end

  def test_concurrency_counter
    get '/index?con=1'
    assert_equal( 200, last_response.status )

    get '/concount?con=1'
    assert_equal( 200, last_response.status )
    assert_equal( 1, last_response.body.to_i )

    get '/concount?con=1'
    assert_equal( 200, last_response.status )
    assert_equal( 1, last_response.body.to_i )

    get '/concount'
    assert_equal( 200, last_response.status )
    assert_equal( 0, last_response.body.to_i )
  end

  def test_index
    get '/index'
    assert_equal( 200, last_response.status )
    assert_match( %r{^text/html(;.*)?}, last_response.content_type )
  end

  def test_atom
    get '/atom.xml'
    assert_equal( 200, last_response.status )
    assert_match( %r{^application/atom\+xml(;.*)?}, last_response.content_type )
  end

  def test_echo_header
    get( '/echo/header/Accept', {}, { 'HTTP_ACCEPT' => 'text/plain' } )
    assert_equal( 'text/plain', last_response.body )
  end

end
