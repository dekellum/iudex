#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2010 David Kellum
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

class TestHttpClient < MiniTest::Unit::TestCase
  include Iudex::Core

  def test_config
    assert( Config.do_http_client_3 )

    called = :not
    Config.setup_http_client_3 do |mgr|
      assert_equal( 100, mgr.manager_params.max_total_connections )
      called = :called
    end
    assert( Config.do_http_client_3 )
    assert_equal( :called, called )

    mgr = Config.do_http_client_3
    assert( mgr )
    mgr.start
    assert( mgr.client )
    mgr.shutdown

  end
end
