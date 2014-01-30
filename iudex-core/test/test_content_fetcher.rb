#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2014 David Kellum
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
require 'iudex-core'

module TestHTTPMocks
  include Iudex::Filter

  class MockHTTPClient
    include Iudex::HTTP::HTTPClient
    def create_session
      MockSession.new
    end

    def request( session, handler )
      session.execute( handler )
    end
  end

  WEAK_ETAG = 'W/"weak-etag"'

  class MockSession < Iudex::HTTP::HTTPSession
    import 'java.nio.ByteBuffer'
    include Iudex::HTTP

    attr_writer :status

    def initialize
      super()
      @status = 200
      @request_headers = []
    end

    def addRequestHeader( h )
      @request_headers << h
    end

    def requestHeaders
      @request_headers
    end

    def responseHeaders
      [ Header.new( "ETag", WEAK_ETAG ) ]
    end

    def statusCode
      @status
    end

    def responseBody
      ByteBuffer::wrap( "BODY".to_java_bytes )
    end

    def statusText
      "status text"
    end

    def execute( handler )
      handler.session_completed( self )
    end

    def close
    end
  end

  class TestReceiver < FilterBase
    def initialize( &block )
      @block = block
      @log = RJack::SLF4J[ self.class ]
    end

    def filter( out )
      pretty_log( out )
      @block.call( out )
    end

    def pretty_log( out )
      @log.debug do
        p = 0
        rep = out.to_s.gsub( /{/ ) do
          "\n" + ( '  ' * (p += 1) ) + "{ "
        end
        rep.gsub( /}/, " }" )
      end
    end
  end

  import 'iudex.core.VisitCounter'

  class TestVisitCounter
    include VisitCounter
    attr_reader :released

    def add( order )
    end

    def release( acquired, newOrder )
      @released = acquired.url
    end
  end

end

class TestContentFetcher < MiniTest::Unit::TestCase
  include Iudex::HTTP
  include Iudex::Core
  include Iudex::Core::Filters
  include Iudex::Filter::Core
  include Gravitext::HTMap

  UniMap.define_accessors

  include TestHTTPMocks

  def setup
    @fetcher = nil
  end

  DEFAULT_URL   = "http://gravitext.com/test"

  def test_simple
    inp = create_content
    fetch( inp ) do |out|
      assert_equal( DEFAULT_URL, out.url.to_s )
      assert_equal( 200, out.status )
      assert_equal( WEAK_ETAG, out.etag )
      assert_equal( "User-Agent", out.request_headers.first.name )
      assert( out.source )
    end
  end

  def test_dynamic_request_headers
    inp = create_content
    inp.request_headers = [ Header.new( "Dynamic", "value" ),
                            Header.new( "User-Agent", "override" ) ]
    fetch( inp ) do |out|
      hhash = out.request_headers.inject( {} ) do |m,h|
        m[ h.name ] = h.value
        m
      end
      assert_equal( { "Dynamic" => "value",
                      "User-Agent" => "override" }, hhash )
    end
  end

  def test_304
    client = MockHTTPClient.new
    def client.request( session, handler )
      session.status = 304
      handler.session_completed( session )
    end
    fetch( create_content, client ) do |out|
      assert_equal( DEFAULT_URL, out.url.to_s )
      assert_equal( 304, out.status )
    end
  end

  import "java.net.UnknownHostException"

  def test_connect_error
    client = MockHTTPClient.new
    def client.create_session
      s = MockSession.new
      def s.execute( handler )
        self.error = UnknownHostException.new( "foobar.com" )
        handler.session_completed( self )
      end
      def s.statusCode
        -1
      end
      def s.responseHeaders
        nil
      end
      s
    end
    fetch( create_content, client ) do |out|
      assert_equal( -1, out.status )
      assert_nil( out.response_headers )
      assert( out.reason =~ /UnknownHostException/ )
    end
  end

  def fetch( content, client = MockHTTPClient.new, &block )
    rec = TestReceiver.new( &block )
    counter = TestVisitCounter.new
    cf = ContentFetcher.new( client,
                             counter,
                             FilterChain.new( "test-rec", [ rec ] ) )
    cf.request_headers = [ Header.new( "User-Agent", "default" ) ]
    cf.filter( content )
  end

  def create_content( url = DEFAULT_URL )
    content = UniMap.new
    content.url = visit_url( url )
    content
  end

  def visit_url( url )
    VisitURL.normalize( url )
  end

end
