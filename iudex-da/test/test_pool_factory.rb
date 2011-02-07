#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2011 David Kellum
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
require 'iudex-da/ar'

require 'iudex-da'
require 'iudex-da/pool_data_source_factory'

class TestPoolFactory < MiniTest::Unit::TestCase
  include Iudex::DA
  import 'org.apache.commons.dbutils.ResultSetHandler'
  import 'org.apache.commons.dbutils.QueryRunner'

  def setup
    @factory = PoolDataSourceFactory.new( :loglevel => 2 )
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
