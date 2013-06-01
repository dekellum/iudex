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
require 'iudex-http-test/broken_server'

require 'iudex-async-httpclient'
require 'thread'

class TestHTTPClient < MiniTest::Unit::TestCase
  include Iudex
  include Iudex::HTTP
  include Iudex::HTTP::Test
  include Helper

  import 'com.ning.http.client.MaxRedirectException'
  import 'java.util.concurrent.TimeoutException'
  import 'java.net.ConnectException'
  import 'java.io.IOException'

  CustomUnit.register

  def setup
    server # make sure jetty starts, for cosmetic log output
  end

  def test_default_config
    client = AsyncHTTPClient.create_client
    client.close
    pass
  end

  import 'java.util.concurrent.ThreadPoolExecutor'
  import 'java.util.concurrent.ArrayBlockingQueue'
  import 'java.util.concurrent.TimeUnit'

  def test_custom_executor

    executor = ThreadPoolExecutor.new( 1, 10,
                                       10, TimeUnit::SECONDS,
                                       ArrayBlockingQueue.new( 20 ) )

    with_new_client( :executor_service => executor ) do |client|

      with_session_handler( client, "/index" ) do |s,x|
        assert_equal( 200, s.status_code )
      end
    end

    executor.shutdown
  end

  def test_200
    with_new_client do |client|

      with_session_handler( client, "/index" ) do |s,x|
        assert_equal( 200, s.status_code )
        assert_match( /Test Index Page/, s.response_stream.to_io.read )
      end

      with_session_handler( client, "/atom.xml" ) do |s,x|
        assert_equal( 200, s.status_code )
        cl = find_header( s.response_headers, "Content-Length" )
        assert_operator( cl.to_i, :>, 10_000 )
      end

    end
  end

  def test_correct_type
    with_new_client do |client|
      client.accepted_content_types = ContentTypeSet.new( [ "text/html" ] )
      with_session_handler( client, "/index" ) do |s,x|
        assert_equal( 200, s.status_code )
        assert_nil( x )
        assert_match( /^text\/html/,
                      find_header( s.response_headers, 'Content-Type' ) )
      end
    end
  end

  def test_headers
    req,rsp = nil
    with_new_client do |client|
      with_session_handler( client,
                            "/echo/header/Accept?noop=3",
                            true,
                            { 'Accept' => 'text/plain;moo' } ) do |s,x|
        assert_equal( 200, s.status_code )
        assert_equal( 'GET /echo/header/Accept?noop=3',
                      find_header( s.request_headers, "Request-Line" ) )
        assert_equal( 'text/plain;moo',
                      find_header( s.request_headers, 'Accept' ) )
        assert_equal( 'localhost:9232',
                      find_header( s.request_headers, 'Host' ) )

        assert_match( /^text\/plain/,
                      find_header( s.response_headers, 'Content-Type' ) )
        assert_match( /^text\/plain;moo$/, s.response_stream.to_io.read )
      end
    end
  end

  def test_unknown_host
    with_new_client do |client|
      with_session_handler( client,
                            "http://9xa9.a7v6a7lop-9m9q-w12.com" ) do |s,x|
        assert_instance_of( ConnectException, x )
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

    #FIXME: Looks like request_timeout is used as this timeout as well.
    with_new_client( :connection_timeout_in_ms => 100,
                     :request_timeout_in_ms    => 200 ) do |client|
      with_session_handler( client,
                            "http://localhost:9233/" ) do |s,x|
        assert_instance_of( TimeoutException, x )
      end
    end
  ensure
    bs.stop
  end

  def test_404
    with_new_client do |client|
      with_session_handler( client, "/not-found" ) do |s,x|
        assert_equal( 404, s.status_code )
      end
    end
  end

  def test_304
    with_new_client do |client|
      client.accepted_content_types = ContentTypeSet.new( [ "text/html" ] )
      with_session_handler( client, "/304" ) do |s,x|
        assert_equal( 304, s.status_code )
      end
    end
  end

  def test_timeout
    with_new_client( :request_timeout_in_ms => 400 ) do |client|
      with_session_handler( client, "/index?sleep=1.0" ) do |s,x|
        assert_instance_of( TimeoutException, x )
      end
    end
    sleep 0.70 # FIXME: Account for test server delay. Should be
               # joined instead.
  end

  def test_redirect
    with_new_client( :follow_redirects => true ) do |client|
      with_session_handler( client, "/" ) do |s,x|
        assert_equal( 200, s.status_code )
        assert_equal( 'http://localhost:9232/index', s.url )
      end
    end
  end

  def test_redirect_with_query_string
    with_new_client( :follow_redirects => true ) do |client|
      with_session_handler( client, "/redirects/multi/2?sleep=0" ) do |s,x|
        assert_equal( 200, s.status_code )
        assert_equal( 'http://localhost:9232/redirects/multi/1?sleep=0',
                      s.url )
        assert_equal( 'GET /redirects/multi/1?sleep=0',
                      find_header( s.request_headers, "Request-Line" ) )
      end
    end
  end

  def test_multi_redirect
    with_new_client( :follow_redirects => true,
                     :maximum_number_of_redirects => 7 ) do |client|
      with_session_handler( client, "/redirects/multi/6" ) do |s,x|
        assert_equal( 200, s.status_code )
        assert_nil x
      end
    end
  end

  def test_unfollowed_301_redirect
    with_new_client( :follow_redirects => false ) do |client|
      with_session_handler( client, "/301" ) do |s,x|
        assert_equal( 301, s.status_code )
        lh = find_header( s.response_headers, "Location" )
        assert_match( %r{/index$}, lh )
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
    with_new_client( :request_timeout_in_ms => 500 ) do |client|
      with_session_handler( client, "/redirects/multi/3?sleep=0.40" ) do |s,x|
        assert_instance_of( TimeoutException, x )
      end
      sleep 0.80
    end
  end

  def test_bad_server_response
    bs = BrokenServer.new
    bs.start

    sthread = Thread.new do
      bs.accept { |sock| sock.write "FU Stinky\r\n" }
    end

    #FIXME: IllegalArgumentException on bad HTTP response line?
    with_new_client do |client|
      with_session_handler( client, "http://localhost:9233/" ) do |s,x|
        assert_instance_of( Java::java.lang.IllegalArgumentException, x )
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

    with_new_client( :connection_timeout_in_ms => 100,
                     :request_timeout_in_ms    => 100 ) do |client|
      with_session_handler( client, "http://localhost:9233/" ) do |s,x|
        assert_instance_of( IOException, x )
      end
    end

    sthread.join

  ensure
    bs.stop
  end

  def test_maximum_connections_total
    skip( "IOException: Too many connections; expected blocking" )
    with_new_client( :maximum_connections_total => 1 ) do |client|

      resps = []
      sessions = (1..7).map do |i|
        with_session_handler( client, "/index?sleep=2&con=1&i=#{i}",
                              false ) do |s,x|
          resps << [ s.status_code, x ]
        end
      end

      sessions.each { |s| s.wait_for_completion }

      assert_equal( [ [ 200, nil ] ] * 7, resps )
    end
  end

  def test_maximum_connections_per_host
    skip( "max_connections_per_host not honored?" )
    with_new_client( :maximum_connections_per_host => 1 ) do |client|

      resps = []
      sessions = (1..7).map do |i|
        with_session_handler( client, "/index?sleep=2&con=1&i=#{i}",
                              false ) do |s,x|
          resps << [ s.status_code, x ]
        end
      end

      sessions.each { |s| s.wait_for_completion }

      assert_equal( [ [ 200, nil ] ] * 7, resps )
    end
  end

  def test_abort_when_too_large
    with_new_client do |client|
      with_session_handler( client, "/giant" ) do |s,x|
        assert_nil( x )
        assert_equal( HTTPSession::TOO_LARGE, s.status_code )
      end
    end
  end

  def test_abort_when_too_large_length
    with_new_client do |client|
      client.max_content_length = 1
      with_session_handler( client, "/atom.xml" ) do |s,x|
        assert_nil( x )
        assert_equal( HTTPSession::TOO_LARGE_LENGTH, s.status_code )
      end
    end
  end

  def test_abort_when_wrong_type
    with_new_client do |client|
      client.accepted_content_types = ContentTypeSet.new( [ "gold/*" ] )
      with_session_handler( client, "/giant" ) do |s,x|
        assert_nil( x )
        assert_equal( HTTPSession::NOT_ACCEPTED, s.status_code )
      end
    end
  end

  def with_session_handler( client, uri, wait = true, headers = {}, &block )
    session = client.create_session
    uri = "http://localhost:#{server.port}#{uri}" unless uri =~ /^http:/
    session.url = uri
    headers.each do |k,v|
      session.add_request_header( Java::iudex.http.Header.new( k, v ) )
    end
    handler = TestHandler.new( &block )
    client.request( session, handler )
    if wait
      session.wait_for_completion
      assert handler.called?
      session.close
    end
    session
  end

  def with_new_client( opts = {} )
    opts = { :max_request_retry => 0 }.merge( opts )

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

    def sessionCompleted( session )
      forward( session, session.error )
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
        flunk "Handler called twice!"
      end
    rescue NativeException => x
      @failure = x.cause
    rescue Exception => x
      @failure = x
    end

  end

  def find_header( headers, name )
    cl = headers.find { |h| h.name.to_s == name }
    cl && cl.value.to_s
  end

end
