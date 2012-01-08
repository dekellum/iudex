#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2012 David Kellum
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

require 'iudex-rome'

class TestRome < MiniTest::Unit::TestCase
  include Iudex::Core
  include Iudex::ROME
  include Gravitext::HTMap

  import 'java.nio.ByteBuffer'
  import 'com.gravitext.util.Charsets'

  UniMap.define_accessors

  SIMPLE_RSS = File.join( File.dirname( __FILE__ ), 'simple_rss.xml' )

  def test_parse
    parser = RomeFeedParser.new
    map = UniMap.new

    rss_bytes = ByteBuffer.wrap( File.read( SIMPLE_RSS ).to_java_bytes )
    source = ContentSource.new( rss_bytes )
    source.set_default_encoding( Charsets::UTF_8 )
    map.source = source

    parser.filter( map )

    assert_equal( "Channel Title", map.title )

    assert( item = map.references.first )

    assert_equal( "Item Title", item.title )
    assert_equal( "http://iudex.gravitext.com/test/item/1.html" +
                  "?click_track=a79bna7",
                  item.url.to_s )
    assert( item.pub_date )
    assert_equal( item.pub_date, item.ref_pub_date )
    assert( item.summary )

  end

end
