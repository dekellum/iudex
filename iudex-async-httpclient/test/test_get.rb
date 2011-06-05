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
  include Iudex::HTTP::Test
  include Helper
  CustomUnit.register

  import "iudex.http.BaseResponseHandler"

  class TestHandler < BaseResponseHandler
    include MiniTest::Assertions
    def handleSuccess( session )
      puts "Response code: #{session.response_code}"
    end
    def handleError( session, code )
      assert_equal( code, session.response_code )
      puts "Response code: #{session.response_code}"
    end
  end

  def test_get

    cfg = AsyncHTTPClient.create_client_config
    aclient = RJack::AsyncHTTPClient::AsyncHttpClient.new( cfg )
    begin

      client = AsyncHTTPClient::Client.new( aclient )
      session = client.create_session

      session.method = HTTP::HTTPSession::Method::GET
      session.url = "http://localhost:#{server.port}/"

      client.request( session, TestHandler.new )

      sleep( 10 )

    ensure
      aclient.close
    end

  end
end
