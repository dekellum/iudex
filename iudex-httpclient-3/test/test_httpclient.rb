#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2011 David Kellum
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

require 'iudex-httpclient-3'

require 'iudex-http-test/helper'
require 'iudex-http-test/broken_server'

require 'thread'

class TestHTTPClient < MiniTest::Unit::TestCase
  include Iudex
  include Iudex::HTTP
  include Iudex::HTTP::Test
  include Helper

  import 'java.net.ConnectException'
  import 'java.net.UnknownHostException'
  import 'java.net.SocketTimeoutException'
  import 'java.net.SocketException'
  import 'org.apache.commons.httpclient.NoHttpResponseException'

  CustomUnit.register

  def setup
    server # make sure jetty starts, for cosmetic log output
  end

  def test_config

    called = :not
    Hooker.with( :iudex ) do |h|
      h.setup_http_client_3 do |mgr|
        assert_equal( 100, mgr.manager_params.max_total_connections )
        called = :called
      end
    end

    mgr = HTTPClient3.create_manager
    assert( mgr )
    assert_equal( :called, called )

    mgr.start
    assert( mgr.client )
    mgr.shutdown

  end

  def test_200
    with_new_client do |client|

      with_session_handler( client, "/index" ) do |s,x|
        assert_equal( 200, s.response_code )
        assert_match( /Test Index Page/, s.response_stream.to_io.read )
      end

      with_session_handler( client, "/atom.xml" ) do |s,x|
        assert_equal( 200, s.response_code )
        body = s.response_stream.to_io.read
        assert_operator( body.length, :>, 10_000 )
      end

    end
  end

  def test_unknown_host
    with_new_client do |client|
      with_session_handler( client,
                            "http://9xa9.a7v6a7lop-9m9q-w12.com" ) do |s,x|
        assert_instance_of( UnknownHostException, x )
      end
    end
  end

  def test_local_connection_refused
    with_new_client do |client|
      with_session_handler( client,
                            "http://localhost:54929/" ) do |s,x|
        assert_instance_of( ConnectException, x )
      end
    end
  end

  def test_connection_timeout
    bs = BrokenServer.new
    bs.start

    with_new_client do |client|
      with_session_handler( client,
                            "http://localhost:19293/" ) do |s,x|
        assert_instance_of( SocketTimeoutException, x )
      end
    end
  ensure
    bs.stop
  end

  def test_404
    with_new_client do |client|
      with_session_handler( client, "/not-found" ) do |s,x|
        assert_equal( 404, s.response_code )
      end
    end
  end

  def test_timeout
    with_new_client do |client|
      with_session_handler( client, "/index?sleep=1.0" ) do |s,x|
        assert_instance_of( SocketTimeoutException, x )
      end
    end
    sleep 0.65 # FIXME: Account for test server delay. Should be
               # joined instead.
  end

  def test_redirect
    settings = lambda do |mgr|
      mgr.client_params.set_int_parameter( "http.protocol.max-redirects", 7 )
    end

    with_new_client( settings ) do |client|
      with_session_handler( client, "/redirects/multi/6" ) do |s,x|
        assert_equal( 200, s.response_code )
        assert_nil x
      end
    end
  end

  def test_unfollowed_301_redirect
    settings = lambda do |mgr|
      mgr.client_params.set_int_parameter( "http.protocol.max-redirects", 0 )
    end

    with_new_client( settings ) do |client|
      with_session_handler( client, "/301" ) do |s,x|
        assert_equal( 301, s.response_code )
        lh = s.response_headers.find { |h| "Location" == h.name.to_s }
        assert_match( %r{/index$}, lh.value )
      end
    end
  end

  def test_too_many_redirects
    settings = lambda do |mgr|
      mgr.client_params.set_int_parameter( "http.protocol.max-redirects", 19 )
    end

    with_new_client( settings ) do |client|
      with_session_handler( client, "/redirects/multi/20" ) do |s,x|
         assert_equal( 302, s.response_code )
      end
    end
  end

  def test_redirect_timeout
    skip( "Unreliable timeout with redirects, timing dependent" )
    with_new_client() do |client|
      with_session_handler( client, "/redirects/multi/3?sleep=0.40" ) do |s,x|
        assert_instance_of( SocketTimeoutException, x )
      end
      sleep 0.75
    end
  end

  def test_bad_server_response
    bs = BrokenServer.new
    bs.start

    sthread = Thread.new do
      bs.accept { |sock| sock.write "FU Stinky\r\n" }
    end

    #FIXME: SocketTimeoutException on bad HTTP response line?
    with_new_client do |client|
      with_session_handler( client, "http://localhost:19293/" ) do |s,x|
        assert_instance_of( SocketTimeoutException, x )
      end
    end

    sthread.join

  ensure
    bs.stop
  end

  def test_empty_server_response
    bs = BrokenServer.new
    bs.start

    sthread = Thread.new do
      bs.accept { |sock| sock.close }
    end

    with_new_client do |client|
      with_session_handler( client, "http://localhost:19293/" ) do |s,x|
        assert( [ NoHttpResponseException, SocketException ].include?( x.class ) )
        #FIXME: One or the other, timing dependent!?
      end
    end

    sthread.join

  ensure
    bs.stop
  end

  def with_session_handler( client, uri, &block )
    session = client.create_session
    uri = "http://localhost:#{server.port}#{uri}" unless uri =~ /^http:/
    session.url = uri
    handler = TestHandler.new( &block )
    client.request( session, handler )
    assert handler.called?
    session.close
    session
  end

  def with_new_client( mgr_proc = nil )
    # Default manager config
    mgr = HTTPClient3.create_manager
    mgr.client_params.set_parameter(
      RJack::HTTPClient3::HttpMethodParams::RETRY_HANDLER,
      RJack::HTTPClient3::DefaultHttpMethodRetryHandler.new( 0, false ) )
    mgr.client_params.connection_manager_timeout = 500 #ms
    mgr.client_params.so_timeout = 500 #ms
    # Overrides via proc
    mgr_proc.call( mgr ) if mgr_proc

    mgr.start
    begin
      yield HTTPClient3::HTTPClient3.new( mgr.client )
    ensure
      mgr.shutdown
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
