#!/usr/bin/env jruby

$LOAD_PATH.unshift File.join( File.dirname(__FILE__), "..", "lib" )

require 'rubygems'
require 'iudex'

require 'test/unit'

class TestPoolFactory < Test::Unit::TestCase
  include Iudex
  import 'org.apache.commons.dbutils.ResultSetHandler'
  import 'org.apache.commons.dbutils.QueryRunner'

  def setup
    @factory = PoolDataSourceFactory.new( 
      { 'username' => 'david',
        'database' => 'crawler_test' } )
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
