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

  def setup
    Url.truncate
    @factory = PoolDataSourceFactory.new( :loglevel => 4 )
    @data_source = @factory.create
    @mapper = ContentMapper.new( keys( :uhash, :domain, :url, :type,
                                       :priority, :next_visit_after ) )
    @poller = WorkPoller.new( @data_source, @mapper )
  end

  def teardown
    @factory.close
    @date_source = nil
  end

  def test_poll
    urls = [ [ "http://foo.gravitext.com/bar/1", 11 ],
             [ "http://gravitext.com/2",         10 ],
             [ "http://hometown.com/33",         10 ] ]

    urls.each do | u, p |
      Url.create( :visit_url => u, :priority => p, :type => "PAGE"  )
    end

    pos = 0
    @poller.poll.each do |map|
      assert_equal( urls[ pos ][ 0 ], map.url.url )
      pos += 1
    end
    assert_equal( 3, pos )
  end

end