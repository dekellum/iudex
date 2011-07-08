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
    @rlock = Mutex.new
    server # make sure jetty starts, for cosmetic log output
  end

  def test_default_config
    client = JettyHTTPClient.create_client
    client.close
    pass
  end

  def test_200
    #Note: "atom fails on jruby 1.5.6, client, 32bit JVMs"
    with_new_client do |client|

      with_session_handler( client, "/index" ) do |s,x|
        output_bomb( s ) unless s.response_code == 200
        assert_equal( 200, s.response_code, "see bomb.out" )
        assert_match( /Test Index Page/, s.response_stream.to_io.read )
      end

      with_session_handler( client, "/atom.xml" ) do |s,x|
        output_bomb( s ) unless s.response_code == 200
        assert_equal( 200, s.response_code, "see bomb.out" )
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
        assert_equal( 404, s.response_code )
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
        assert_equal( 302, s.response_code, x )
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
            resps << [ s.response_code, x ]
            output_bomb( s ) if s.response_code != 200
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
            resps << [ s.response_code, x ]
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
        assert_equal( -11, s.response_code )
      end
    end
  end

  def test_abort_when_too_large_length
    with_new_client do |client|
      client.max_content_length = 1
      with_session_handler( client, "/atom.xml" ) do |s,x|
        assert_nil( x )
        assert_equal( -10, s.response_code )
      end
    end
  end

  def test_abort_when_wrong_type
    with_new_client do |client|
      client.accepted_content_types = ContentTypeSet.new( [ "gold/*" ] )
      with_session_handler( client, "/giant" ) do |s,x|
        assert_nil( x )
        assert_equal( -20, s.response_code )
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

  def with_session_handler( client, uri, wait = true, &block )
    session = client.create_session
    uri = "http://localhost:#{server.port}#{uri}" unless uri =~ /^http:/
    session.url = uri
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
      yield client
    ensure
      client.close
    end
  end

  class TestHandler < BaseResponseHandler
    include RJack
    def initialize( &block )
      @block = block
      @failures = []
      @log = SLF4J[ self.class ]
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
      ff = @failures.shift
      @failures.each do |f|
        @log.error( "Additional failure: ", f )
      end
      raise ff if ff
      @block.nil?
    end

    def forward( s, x = nil )
      b, @block = @block, nil
      if b
        b.call( s, x )
      else
        @failures << x if x
      end
    rescue NativeException => x
      @failures << x.cause
    rescue Exception => x
      @failures << x
    end

  end

end

if ARGV.delete( '--loop' )
  loop do
    failed = MiniTest::Unit.new.run( ARGV )
    exit!( 1 ) if failed > 0
  end
end
