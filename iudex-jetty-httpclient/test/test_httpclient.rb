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
require 'iudex-http-test/broken_server'

require 'iudex-jetty-httpclient'
require 'thread'

class TestHTTPClient < MiniTest::Unit::TestCase
  include Iudex
  include Iudex::HTTP
  include Iudex::HTTP::Test
  include Helper

  import 'java.util.concurrent.TimeoutException'
  import 'java.net.ConnectException'
  import 'java.net.UnknownHostException'
  import 'java.io.IOException'
  import 'java.io.EOFException'
  import 'java.lang.IllegalStateException'
  import 'java.nio.channels.UnresolvedAddressException'

  CustomUnit.register

  def setup
    server # make sure jetty starts, for cosmetic log output
  end

  def test_default_config
    client = JettyHTTPClient.create_client
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
        assert_operator( cl.value.to_s.to_i, :>, 10_000 )
      end

    end
  end

  import 'java.util.concurrent.ThreadPoolExecutor'
  import 'java.util.concurrent.ArrayBlockingQueue'
  import 'java.util.concurrent.TimeUnit'
  import 'org.eclipse.jetty.util.thread.ExecutorThreadPool'

  def test_custom_executor
    #FIXME: A bit shaky, fails under 3 threads?
    executor = ThreadPoolExecutor.new( 3, 10,
                                       10, TimeUnit::SECONDS,
                                       ArrayBlockingQueue.new( 10 ) )
    pool = ExecutorThreadPool.new( executor )

    with_new_client( :thread_pool => pool ) do |client|
      with_session_handler( client, "/index" ) do |s,x|
        assert_equal( 200, s.response_code )
      end
    end

    pool.stop
  end

  def test_unknown_host
    with_new_client( :timeout => 2_000 ) do |client|
      with_session_handler( client,
                            "http://9xa9.a7v6a7lop-9m9q-w12.com" ) do |s,x|
        assert_includes( [ UnresolvedAddressException,
                           UnknownHostException ], x.class )
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
    with_new_client do |client|
      with_session_handler( client,
                            "http://localhost:19293/" ) do |s,x|
        assert_instance_of( TimeoutException, x )
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
        assert_instance_of( TimeoutException, x )
      end
    end
    sleep 0.70 # FIXME: Account for test server delay. Should be
               # joined instead.
  end

  def test_redirect
    with_new_client( :max_redirects => 8 ) do |client|
      with_session_handler( client, "/redirects/multi/6" ) do |s,x|
        assert_equal( 200, s.response_code )
        assert_nil x
      end
    end
  end

  def test_unfollowed_301_redirect
    with_new_client( :max_redirects => 0 ) do |client|
      with_session_handler( client, "/301" ) do |s,x|
        assert_equal( 301, s.response_code )
        lh = s.response_headers.find { |h| "Location" == h.name.to_s }
        assert_match( %r{/index$}, lh.value.to_s )
      end
    end
  end

  def test_too_many_redirects
    with_new_client( :max_redirects => 18 ) do |client|
      #FIXME: One redirect off somewhere? 19 fails.
      with_session_handler( client, "/redirects/multi/20" ) do |s,x|
        assert_equal( 302, s.response_code )
      end
    end
  end

  def test_redirect_timeout
    skip( "Unreliable timeout with redirects, timing dependent" )
    with_new_client do |client|
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
      with_session_handler( client, "http://localhost:19293/" ) do |s,x|
        assert_instance_of( IllegalStateException, x )
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
        assert_instance_of( EOFException, x )
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
          resps << [ s.response_code, x ]
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
          resps << [ s.response_code, x ]
        end
      end

      sessions.each { |s| s.wait_for_completion }

      assert_equal( [ [ 200, nil ] ] * 7, resps )
    end
  end

  def with_session_handler( client, uri, wait = true, &block )
    session = client.create_session
    uri = "http://localhost:#{server.port}#{uri}" unless uri =~ /^http:/
    session.url = uri
    handler = TestHandler.new( &block )
    client.request( session, handler )
    if wait
      session.wait_for_completion
      assert( handler.called?, "Handler should have been called!" )
      session.close
    end
    session
  end

  def with_new_client( opts = {} )
    opts = { :max_retries => 0,
             :timeout => 500,
             :so_timeout => 400,
             :connect_timeout => 300,
             :idle_timeout => 200,
             :connect_blocking => false }.merge( opts )

    client = JettyHTTPClient.create_client( opts )
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
