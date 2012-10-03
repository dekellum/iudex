#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2012 David Kellum
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
  include Iudex::Filter::KeyHelper
  include Iudex::DA
  include Iudex::DA::ORM

  Gravitext::HTMap::UniMap.define_accessors

  URLS = [ [ "http://foo.gravitext.com/bar/1", 11 ],
           [ "http://hometown.com/33",         10 ],
           [ "http://gravitext.com/2",          9 ] ]

  def setup
    Url.truncate

    URLS.each do | u, p |
      Url.create( :visit_url => u, :priority => p, :type => "PAGE"  )
    end

    @factory = PoolDataSourceFactory.new( :loglevel => 4 )
    @data_source = @factory.create
    @mapper = ContentMapper.new( keys( :url, :type, :priority,
                                       :next_visit_after ) )
  end

  def teardown
    @factory.close
    @date_source = nil
  end

  def test_default_poll
    poller = WorkPoller.new( @data_source, @mapper )

    pos = 0
    poller.poll.each do |map|
      assert_equal( URLS[ pos ][ 0 ], map.url.url )
      pos += 1
    end
    assert_equal( 3, pos )
  end

  def test_poll_with_max_priority_urls
    poller = WorkPoller.new( @data_source, @mapper )
    poller.max_priority_urls = 4

    pos = 0
    poller.poll.each do |map|
      assert_equal( URLS[ pos ][ 0 ], map.url.url )
      pos += 1
    end
    assert_equal( 3, pos )
  end

  def test_poll_with_domain_depth
    poller = WorkPoller.new( @data_source, @mapper )
    poller.domain_depth_coef = 0.125
    poller.max_priority_urls = 4

    pos = 0
    poller.poll.each do |map|
      assert_equal( URLS[ pos ][ 0 ], map.url.url )
      pos += 1
    end
    assert_equal( 3, pos )
  end

  def test_poll_with_domain_depth_only
    poller = WorkPoller.new( @data_source, @mapper )
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
    poller = WorkPoller.new( @data_source, @mapper )
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

  def test_poll_domain_union
    poller = WorkPoller.new( @data_source, @mapper )
    poller.domain_union = [ [ 'google.com', 15000 ],
                            [ nil, 10000 ] ]

    result = poller.poll
    assert_equal( 3, result.size )
  end

  def test_poll_uhash_slice
    poller = WorkPoller.new( @data_source, @mapper )
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
