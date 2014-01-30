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

require 'iudex-da'
require 'iudex-da/pool_data_source_factory'

class TestPoolFactory < MiniTest::Unit::TestCase
  include Iudex::DA
  include Iudex::Core

  import 'org.apache.commons.dbutils.ResultSetHandler'
  import 'org.apache.commons.dbutils.QueryRunner'

  def setup
    @factory = PoolDataSourceFactory.new( :loglevel => 4 )
    @data_source = @factory.create
  end

  def teardown
    @factory.close
    @data_source = nil
  end

  # Really just want to test the factory and data_source but this
  # makes a fine demonstration of dbutils query runner "just working"
  # via ruby.
  def test_query_runner
    assert( @data_source )
    qrun = QueryRunner.new( @data_source )

    url = VisitURL.normalize( "http://gravitext.com/test" )

    qrun.update( "TRUNCATE urls;" )

    c = qrun.update( "INSERT into urls (uhash, url, domain, type ) " +
                     "VALUES (?,?,?,?);",
                     url.uhash, url.url, url.domain, "PAGE" )
    assert_equal( 1, c )

    out_domain = nil
    qrun.query( "SELECT * FROM urls WHERE uhash = ?", url.uhash ) do |rs|
      while rs.next
        out_domain = rs.string( 'domain' )
      end
    end
    assert_equal( url.domain, out_domain )

    assert_equal( 1, qrun.update( "DELETE from urls;" ) )
  end

end
