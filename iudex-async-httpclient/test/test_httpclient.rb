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

require 'iudex-http-test/helper'

require 'iudex-async-httpclient'

class TestHTTPClient < MiniTest::Unit::TestCase
  include Iudex
  include Iudex::HTTP
  include Iudex::HTTP::Test
  include Helper

  import 'com.ning.http.client.MaxRedirectException'
  import 'java.util.concurrent.TimeoutException'
  import 'java.net.ConnectException'

  CustomUnit.register

  def setup
    server # make sure jetty starts, for cosmetic log output
  end

  def test_default_config
    client = AsyncHTTPClient.create_client
    client.close
    pass
  end

  def test_200
    with_new_client do |client|

      with_session_handler( client, "/index" ) do |s,x|
        assert_equal( 200, s.response_code )
        assert_match( /Test Index Page/, s.response_stream.to_io.read )
      end

      with_session_handler( client, "/atom.xml" ) do |s,x|
        assert_equal( 200, s.response_code )
        cl = s.response_headers.find { |h| "Content-Length" == h.name.to_s }
        assert_operator( cl.value.to_i, :>, 10_000 )
      end

    end
  end

  def test_bad_host
    with_new_client do |client|
      with_session_handler( client,
                            "http://9xa9.a7v6a7lop-9m9q-w12.com" ) do |s,x|
        assert_instance_of( ConnectException, x )
      end
    end
  end

  def test_404
    with_new_client do |client|
      with_session_handler( client, "/not-found" ) do |s,x|
        assert_equal( 404, s.response_code )
      end
    end
  end

  def test_timeout
    ex = nil
    with_new_client( :request_timeout_in_ms => 400 ) do |client|
      with_session_handler( client, "/index?sleep=1.0" ) do |s,x|
        ex = x
      end
    end
    assert_instance_of( TimeoutException, ex )
    sleep 0.70 # FIXME: Account for test server delay. Should be
               # joined instead.
  end

  def test_redirect
    with_new_client( :follow_redirects => true,
                     :maximum_number_of_redirects => 7 ) do |client|
      with_session_handler( client, "/redirects/multi/6" ) do |s,x|
        assert_equal( 200, s.response_code )
        assert_nil x
      end
    end
  end

  def test_unfollowed_301_redirect
    with_new_client( :follow_redirects => false ) do |client|
      with_session_handler( client, "/301" ) do |s,x|
        assert_equal( 301, s.response_code )
        lh = s.response_headers.find { |h| "Location" == h.name.to_s }
        assert_match( %r{/index$}, lh.value )
      end
    end
  end

  def test_too_many_redirects
    with_new_client( :follow_redirects => true,
                     :maximum_number_of_redirects => 19 ) do |client|
      with_session_handler( client, "/redirects/multi/20" ) do |s,x|
        assert_instance_of( MaxRedirectException, x )
      end
    end
  end

  def test_redirect_timeout
    skip( "Unreliable timeout with redirects, timing dependent" )
    ex = nil
    with_new_client( :request_timeout_in_ms => 500 ) do |client|
      with_session_handler( client, "/redirects/multi/3?sleep=0.40" ) do |s,x|
        ex = x
      end
      sleep 0.80
    end
    assert_instance_of( TimeoutException, ex )
  end

  def with_session_handler( client, uri, &block )
    session = client.create_session
    uri = "http://localhost:#{server.port}#{uri}" unless uri =~ /^http:/
    session.url = uri
    handler = TestHandler.new( &block )
    client.request( session, handler )
    session.wait_for_completion
    assert handler.called?
  end

  def with_new_client( opts = {} )
    client = AsyncHTTPClient.create_client( opts )
    begin
      yield client
    ensure
      client.close
    end
  end

  class TestHandler < BaseResponseHandler

    def initialize( &block )
      @block = block
      @failure = nil
    end

    def handleSuccess( session )
      forward( session )
    end
    def handleError( session, code )
      forward( session )
    end
    def handleException( session, exception )
      forward( session, exception )
    end

    def called?
      raise @failure if @failure
      @block.nil?
    end

    def forward( s, x = nil )
      b, @block = @block, nil
      if b
        b.call( s, x )
      else
        @failure = x if x
      end
    rescue NativeException => x
      @failure = x.cause
    rescue Exception => x
      @failure = x
    end

  end

end
