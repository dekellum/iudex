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

RJack::Logback.config_console( :stderr => true, :mdc => "uhash",
                               :level => RJack::Logback::DEBUG )

require 'iudex-da'
require 'iudex-da/pool_data_source_factory'
require 'iudex-da/filter_chain_factory'
require 'iudex-httpclient-3'

class TestFilterChainFactory < MiniTest::Unit::TestCase
  include Iudex::Core
  include Iudex::DA
  include Iudex::DA::Filters

  import 'iudex.httpclient3.HTTPClient3'

  def test_filter
    fcf = FilterChainFactory.new( "test-da" )
    fcf.data_source = PoolDataSourceFactory.new.create

    http_mf = RJack::HTTPClient3::ManagerFacade.new
    http_mf.start

    fcf.http_client = HTTPClient3.new( http_mf.client )

    fcf.add_summary_reporter
    fcf.add_by_filter_reporter
    fcf.open
    fcf.close

    http_mf.shutdown
  end

end
