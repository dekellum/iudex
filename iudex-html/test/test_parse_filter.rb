#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2010 David Kellum
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

class TestParseFilter < MiniTest::Unit::TestCase
  include HTMLTestHelper
  include Gravitext::HTMap
  include Iudex::Core
  include Iudex::HTML
  include Iudex::HTML::Filters

  def setup
    @filter = html_parse_filter( :title )
    @filter.min_parse = 0
  end

  def test_marked
    assert( ! marked?( "" ) )
    assert( ! marked?( "simple" ) )
    assert( ! marked?( "<simple" ) )
    assert( ! marked?( "x < y" ) )
    assert( ! marked?( "AT&T" ) )
    assert( ! marked?( "AT & T;" ) )

    assert(   marked?( "<a>" ) )
    assert(   marked?( "Words &copy; 2010" ) )
    assert(   marked?( "&#xf43e;" ) )
    assert(   marked?( "&#2028;" ) )
    assert(   marked?( "<![CDATA[simple]]>" ) )
    assert(   marked?( "<![CDATA[simple" ) )
    assert(   marked?( "<!-- comment -->" ) )
  end

  def marked?( text )
    @filter.text_marked( text )
  end

  def test_markup
    tests = [ [ "simple",               0, "simple" ],
              [ "<i>inner</i>",         1, nil ],
              [ "&lt;i>inner&lt;/i>",   2, nil ],
              [    "<!--ignore-->text", 1, "text" ],
              [ "&lt;!--ignore-->text", 2, "text" ],
              [ "&lt;",                 1, "<" ],
              [ "&amp;lt;",             2, "<" ] ]

    tests.each do | input, count, out |
      map = UniMap.new
      map.title = input
      assert_equal( count, @filter.parse_loop( map ), input )
      assert_equal( out, map.title && map.title.to_s )
    end
  end

end
