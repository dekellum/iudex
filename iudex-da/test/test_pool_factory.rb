#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (C) 2008-2009 David Kellum
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

$LOAD_PATH.unshift File.join( File.dirname(__FILE__), "..", "lib" )

require 'rubygems'

require 'logback'
Logback.config_console
Logback['Iudex.DA'].level = Logback::DEBUG

require 'iudex-da'
require 'iudex-da/pool_date_source_factory'

require 'test/unit'

class TestPoolFactory < Test::Unit::TestCase
  include Iudex::DA
  import 'org.apache.commons.dbutils.ResultSetHandler'
  import 'org.apache.commons.dbutils.QueryRunner'

  def setup
    @factory = PoolDataSourceFactory.new( 'loglevel' => 2 )
    @data_source = @factory.create
  end

  def teardown
    @factory.close
    @data_source = nil
  end

  class TestHandler
    include ResultSetHandler
    def handle( rs )
      while rs.next
        p [ rs.string( 'url' ) ]
      end
      nil
    end
  end

  def test_query
    assert( ! @data_source.nil? )
    qrun = QueryRunner.new( @data_source )
    qrun.query( "SELECT url FROM urls WHERE uhash IN ('uRlU1h_YL-NvooSv2i98Rd3', 'notthere' );",
                TestHandler.new )
  end

end
