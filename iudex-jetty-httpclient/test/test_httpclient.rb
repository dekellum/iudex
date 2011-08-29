#!/usr/bin/env jruby

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
require 'cgi'
require 'uri'

class TestHTTPClient < MiniTest::Unit::TestCase
  include Iudex
  include Iudex::HTTP
  include Iudex::HTTP::Test
  include Helper

  import 'java.util.concurrent.TimeoutException'
  import 'java.net.ConnectException'
  import 'java.net.UnknownHostException'
  import 'java.net.URISyntaxException'
  import 'java.io.IOException'
  import 'java.io.EOFException'
  import 'java.lang.IllegalStateException'
  import 'java.nio.channels.UnresolvedAddressException'
  import 'iudex.http.HostAccessListener'

  CustomUnit.register

  def setup
    @rlock = Mutex.new
    server # make sure jetty starts, for cosmetic log output
  end

  def test_default_config
    client = JettyHTTPClient.create_client
    client.close
    pass
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
        assert_equal( 200, s.status_code )
      end
    end

    pool.stop
  end

  def test_200
    with_new_client do |client|

      with_session_handler( client, "/index" ) do |s,x|
        output_bomb( s ) unless s.status_code == 200
        assert_equal( 200, s.status_code, "see bomb.out" )
        assert_match( /Test Index Page/, s.response_stream.to_io.read )
      end

      with_session_handler( client, "/atom.xml" ) do |s,x|
        output_bomb( s ) unless s.status_code == 200
        assert_equal( 200, s.status_code, "see bomb.out" )
        cl = find_header( s.response_headers, "Content-Length" )
        assert_operator( cl.to_i, :>, 10_000 )
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
        assert_equal( 'localhost:19292',
                      find_header( s.request_headers, 'Host' ) )

        assert_match( /^text\/plain/,
                      find_header( s.response_headers, 'Content-Type' ) )
        assert_match( /^text\/plain;moo$/, s.response_stream.to_io.read )
      end
    end
  end

  def test_unknown_host
    with_new_client( :timeout         => 12_000,
                     :connect_timeout => 10_000,
                     :so_timeout      => 10_000,
                     :idle_timeout    => 10_000 ) do |client|
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
    with_new_client( :short => true ) do |client|
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
        assert_equal( 404, s.status_code )
      end
    end
  end

  def test_timeout
    with_new_client( :short => true ) do |client|
      with_session_handler( client, "/index?sleep=1.0" ) do |s,x|
        assert_instance_of( TimeoutException, x )
      end
    end
    sleep 0.70 # FIXME: Account for test server delay. Should be
               # joined instead.
  end

  def test_redirect
    with_new_client do |client|
      with_session_handler( client, "/" ) do |s,x|
        assert_equal( 200, s.status_code )
        assert_equal( 'http://localhost:19292/index', s.url )
      end
    end
  end

  def test_redirect_with_query_string
    with_new_client do |client|
      with_session_handler( client, "/redirects/multi/2?sleep=0" ) do |s,x|
        assert_equal( 200, s.status_code )
        assert_equal( 'http://localhost:19292/redirects/multi/1?sleep=0',
                      s.url )
        assert_equal( 'GET /redirects/multi/1?sleep=0',
                      find_header( s.request_headers, "Request-Line" ) )
      end
    end
  end

  def test_redirect_multi_host
    with_new_client do |client|
      rurl = 'http://127.0.0.1:19292/index'
      rurl_e = CGI.escape( rurl )
      with_session_handler( client, "/redirect?loc=#{rurl_e}" ) do |s,x|
        assert_equal( 200, s.status_code )
        assert_equal( rurl, s.url )
      end
    end
  end

  def test_redirect_bad_host
    with_new_client do |client|
      with_session_handler( client,
                            '/redirect?loc=http://\bad.com/' ) do |s,x|
        assert_equal( -1, s.status_code )
        assert_instance_of( URISyntaxException, x )
      end
    end
  end

  def test_multi_redirect
    with_new_client( :max_redirects => 8 ) do |client|
      with_session_handler( client, "/redirects/multi/6" ) do |s,x|
        assert_equal( 200, s.status_code )
        assert_nil x
      end
    end
  end

  def test_unfollowed_301_redirect
    with_new_client( :max_redirects => 0 ) do |client|
      with_session_handler( client, "/301" ) do |s,x|
        assert_equal( 301, s.status_code )
        lh = find_header( s.response_headers, "Location" )
        assert_match( %r{/index$}, lh )
      end
    end
  end

  def test_too_many_redirects
    with_new_client( :max_redirects => 18 ) do |client|
      #FIXME: One redirect off somewhere? 19 fails.
      with_session_handler( client, "/redirects/multi/20" ) do |s,x|
        assert_equal( 302, s.status_code, x )
      end
    end
  end

  def test_redirect_timeout
    skip( "Unreliable timeout with redirects, timing dependent" )
    with_new_client( :short => true ) do |client|
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

    #FIXME: IllegalStateException on bad HTTP response line?
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

  def test_concurrent
    with_new_client( :timeout         => 18_000,
                     :connect_timeout => 15_000,
                     :so_timeout      => 12_000,
                     :idle_timeout    => 12_000,
                     :max_connections_per_address => 4 ) do |client|

      resps = []
      sessions = (1..19).map do |i|
        with_session_handler( client, "/index?sleep=0.05&i=#{i}",
                              false ) do |s,x|
          sync do
            resps << [ s.status_code, x ]
            output_bomb( s ) if s.status_code != 200
          end
        end
      end

      sessions.each { |s| s.wait_for_completion }

      assert_equal( [ [ 200, nil ] ] * 19, resps )
    end
  end

  def test_maximum_connections_per_address
    with_new_client( :timeout         => 12_000,
                     :connect_timeout => 10_000,
                     :so_timeout      => 10_000,
                     :idle_timeout    => 10_000,
                     :max_connections_per_address => 2 ) do |client|

      resps = []
      sessions = (1..7).map do |i|
        with_session_handler( client, "/index?sleep=0.1&con=2&i=#{i}",
                              false ) do |s,x|
          sync do
            resps << [ s.status_code, x ]
          end
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

  def sync( &block )
    @rlock.synchronize( &block )
  end

  def output_bomb( s )
    File.open( "bomb.out", "w" ) do |fout|
      st = s && s.response_stream
      if st
        fout.puts st.to_io.read
      else
        fout.puts st.to_s
      end
    end
    "See bomb.out"
  end

  def with_session_handler( client, uri, wait = true, headers = {}, &block )
    session = client.create_session
    if uri =~ /^http:/
      @ha_listener.hostChange( URI.parse( uri ).host, nil )
    else
      uri = "http://localhost:#{server.port}#{uri}"
      @ha_listener.hostChange( 'localhost', nil )
    end

    session.url = uri
    headers.each do |k,v|
      session.add_request_header( Java::iudex.http.Header.new( k, v ) )
    end
    handler = TestHandler.new( &block )
    client.request( session, handler )
    if wait
      session.wait_for_completion
      session.close
      assert( handler.called?, "Handler should have been called!" )
    end
    session
  end

  def with_new_client( opts = {} )
    o = if opts.delete( :short )
          { :timeout          => 400,
            :so_timeout       => 200,
            :connect_timeout  => 200,
            :idle_timeout     => 200 }
        else
          { :timeout          => 5000,
            :so_timeout       => 4000,
            :connect_timeout  => 3000,
            :idle_timeout     => 2000 }
        end

    o = o.merge( { :max_retries      => 0,
                   :connect_blocking => false } )
    o = o.merge( opts )

    client = JettyHTTPClient.create_client( o )
    begin
      @ha_listener = HostAccessCounter.new
      client.set_host_access_listener( @ha_listener )

      client.start

      yield client

      @ha_listener.check_hosts do |host,count|
        assert_equal( 0, count, "host: #{host} count: #{count}" )
      end
    ensure
      client.close
    end
  end

  class HostAccessCounter
    include HostAccessListener

    def initialize
      @hosts = Hash.new { |k,v| k[v] = 0 }
      @lock = Mutex.new
    end

    def hostChange( acquired, released )
      @lock.synchronize do
        @hosts[ acquired ] += 1 if acquired
        @hosts[ released ] -= 1 if released
      end
    end

    def check_hosts
      @hosts.each { |h,c| yield( h, c ) }
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

if ARGV.delete( '--loop' )
  loop do
    failed = MiniTest::Unit.new.run( ARGV )
    exit!( 1 ) if failed > 0
  end
end
