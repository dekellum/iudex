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

  import "iudex.http.BaseResponseHandler"
  import 'com.ning.http.client.MaxRedirectException'

  CustomUnit.register

  def setup
    server #start jetty before all tests for cosmetic log output
  end

  def test_default_config
    config = AsyncHTTPClient.create_client_config
    aclient = RJack::AsyncHTTPClient::AsyncHttpClient.new( config )
    aclient.close
    pass
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

  def test_too_many_redirects
    with_new_client( :follow_redirects => true,
                     :maximum_number_of_redirects => 19 ) do |client|

      with_session_handler( client, "/redirects/multi/20" ) do |s,x|
        assert_instance_of( MaxRedirectException, x )
      end
    end
  end

  def with_session_handler( client, uri, &block )
    session = client.create_session
    session.url = "http://localhost:#{server.port}#{uri}" if uri
    handler = TestHandler.new( &block )
    client.request( session, handler )
    session.wait_for_completion
    assert handler.called?
  end

  def with_new_client( opts = {} )
    client = new_client( opts )
    begin
      yield client
    ensure
      client.close
    end
  end

  def new_client( opts = {} )
    cfg = AsyncHTTPClient.create_client_config( opts )
    aclient = RJack::AsyncHTTPClient::AsyncHttpClient.new( cfg )
    AsyncHTTPClient::Client.new( aclient )
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
      b.call( s, x )
    rescue => x
      @failure = x
    end

  end

end
