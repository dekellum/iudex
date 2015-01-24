#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2015 David Kellum
#
# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License.  You may
# obtain a copy of the License at
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

require 'iudex-da'
require 'iudex-da/key_helper'
require 'iudex-da/pool_data_source_factory'
require 'iudex-da/models'

class TestWorkPoller < MiniTest::Unit::TestCase
  include Iudex::Core
  include Iudex::Filter::KeyHelper
  include Iudex::DA
  include Iudex::DA::ORM

  Gravitext::HTMap::UniMap.define_accessors

  URLS = [ [ "http://foo.gravitext.com/bar/1", 11 ],
           [ "http://hometown.com/33",         10 ],
           [ "http://gravitext.com/2",          9, "ALT" ] ]

  def setup
    Url.truncate

    URLS.each do | u, p, t |
      Url.create( :visit_url => u, :priority => p, :type => t || "PAGE"  )
    end

    @factory = PoolDataSourceFactory.new( :loglevel => 4 )
    @data_source = @factory.create
    @mapper = ContentMapper.new( keys( :url, :type, :priority,
                                       :next_visit_after ) )
    @poller = WorkPoller.new( @data_source, @mapper )
  end

  def teardown
    @factory.close
    @date_source = nil
    @poller = nil
  end

  attr_reader :poller

  def test_default_poll
    pos = 0
    poller.poll.each do |map|
      assert_equal( URLS[ pos ][ 0 ], map.url.url )
      pos += 1
    end
    assert_equal( 3, pos )
  end

  def test_poll_with_reserve
    poller.do_reserve = true
    poller.max_urls = 2
    poller.instance = 'test'

    polled = poller.poll
    polled.each_with_index do |map,i|
      assert_equal( URLS[ i ][ 0 ], map.url.url )
    end
    assert_equal( 2, polled.size )
    reserved = polled

    polled = poller.poll
    assert_equal( 1, polled.size )
    assert_equal( URLS[2][0], polled.first.url.url )
    reserved += polled

    RJack::Logback[ 'iudex.da.WorkPoller' ].with_level( :warn ) do
      poller.discard( VisitQueue.new.tap { |q| q.add_all( reserved ) } )
    end
    poller.max_urls = 3

    assert_equal( 3, poller.poll.size )
  end

  def test_poll_with_max_priority_urls
    poller.max_priority_urls = 4

    pos = 0
    poller.poll.each do |map|
      assert_equal( URLS[ pos ][ 0 ], map.url.url )
      pos += 1
    end
    assert_equal( 3, pos )
  end

  def test_poll_with_domain_depth
    poller.domain_depth_coef = 0.125
    poller.max_priority_urls = 4

    pos = 0
    poller.poll.each do |map|
      assert_equal( URLS[ pos ][ 0 ], map.url.url )
      pos += 1
    end
    assert_equal( 3, pos )
  end

  def test_poll_with_domain_depth_reserve
    poller.domain_depth_coef = 0.125
    poller.max_priority_urls = 4
    poller.do_reserve = true
    poller.instance = 'test'

    pos = 0
    poller.poll.each do |map|
      assert_equal( URLS[ pos ][ 0 ], map.url.url )
      pos += 1
    end
    assert_equal( 3, pos )
    RJack::Logback[ 'iudex.da.WorkPoller' ].with_level( :warn ) do
      assert_equal( 3, poller.instance_unreserve )
    end
    assert_equal( 3, poller.poll.size )
    assert_equal( 0, poller.poll.size )
  end

  def test_poll_with_domain_depth_only
    poller.domain_depth_coef = 0.125
    poller.age_coef_1        = 0.0

    pos = 0
    poller.poll.each do |map|
      assert_equal( URLS[ pos ][ 0 ], map.url.url )
      pos += 1
    end
    assert_equal( 3, pos )
  end

  def test_poll_with_domain_group
    poller.do_domain_group = true

    urls = [ [ "http://foo.gravitext.com/bar/1", 11 ],
             [ "http://gravitext.com/2",          9 ],
             [ "http://hometown.com/33",         10 ] ]

    pos = 0
    poller.poll.each do |map|
      assert_equal( urls[ pos ][ 0 ], map.url.url, "pos #{pos}" )
      pos += 1
    end
    assert_equal( 3, pos )
  end

  def test_poll_domain_union_1
    poller.domain_union = [ [ 'gravitext.com', 15000 ] ]

    result = poller.poll
    assert_equal( 2, result.size )
  end

  def test_poll_domain_union_2
    poller.domain_union = [ { :domain => 'gravitext.com', :max => 15000 },
                            {                             :max => 10000 } ]

    result = poller.poll
    assert_equal( 3, result.size )
  end

  def test_poll_domain_union_2_reserve
    poller.do_reserve = true
    poller.domain_union = [ { :domain => 'gravitext.com', :max => 15000 },
                            {                             :max => 10000 } ]

    assert_equal( 3, poller.poll.size )
    assert_equal( 0, poller.poll.size )
  end

  def test_poll_domain_union_3
    poller.domain_union = [ { :domain => 'gravitext.com', :max => 1 },
                            { :domain => 'hometown.com',  :max => 1 },
                            {                             :max => 3 } ]

    result = poller.poll
    assert_equal( 2, result.size )
  end

  def test_poll_domain_union_type_1
    poller.domain_union = [
      { :domain => 'gravitext.com', :type => 'ALT', :max => 15000 } ]

    result = poller.poll
    assert_equal( 1, result.size )
  end

  def test_poll_domain_union_type_2
    poller.domain_union = [
      { :domain => 'gravitext.com', :type => 'ALT', :max => 1 },
      { :domain => 'gravitext.com',                 :max => 1 } ]

    result = poller.poll
    assert_equal( 2, result.size )
  end

  def test_poll_domain_union_type_3
    poller.domain_union = [
      { :domain => 'gravitext.com', :type => 'ALT', :max => 1 },
      { :domain => 'gravitext.com', :type => 'NOT', :max => 1 },
      { :domain => 'gravitext.com',                 :max => 1 } ]

    result = poller.poll
    assert_equal( 2, result.size )
  end

  def test_poll_domain_union_type_4
    poller.domain_union = [
      { :domain => 'gravitext.com', :type => 'ALT', :max => 1 },
      { :domain => 'gravitext.com', :type => 'NOT', :max => 1 },
      { :domain => 'gravitext.com',                 :max => 1 },
      { :domain =>  'hometown.com',                 :max => 1 } ]

    result = poller.poll
    assert_equal( 3, result.size )
  end

  def test_poll_domain_union_type_5
    poller.domain_union = [ { :type => 'ALT', :max => 15000 } ]

    result = poller.poll
    assert_equal( 1, result.size )
  end

  def test_poll_domain_union_type_6
    poller.domain_union = [ { :type => 'ALT', :max => 2 },
                            {                 :max => 3 } ]

    result = poller.poll
    assert_equal( 3, result.size )
  end

  def test_poll_domain_union_type_7
    poller.domain_union = [ { :type => 'ALT', :max => 2 },
                            {                 :max => 1 } ]

    result = poller.poll
    assert_equal( 2, result.size )
  end

  def test_poll_uhash_slice
    poller.uhash_slice = [ 4, 5 ]

    urls = [ [ "http://hometown.com/33",         10 ] ]

    pos = 0
    poller.poll.each do |map|
      assert_equal( urls[ pos ][ 0 ], map.url.url, "pos #{pos}" )
      pos += 1
    end
    assert_equal( 1, pos )
  end

end
