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

class TestOtherTreeFilters < MiniTest::Unit::TestCase
  include HTMLTestHelper
  include Iudex::HTML::Tree
  include Iudex::HTML::Tree::Filters

  def test_non_html_atts_dropped
    # Bogus is dropped already by parser
    html = {}
    html[ :in ] = <<HTML
<div bogus="not html">
 <p>test.</p>
</div>
HTML
    html[ :out ] = cut_atts( html[ :in ], 'bogus' )

    assert_transform( html ) #identity
  end

  def test_drop_some
    html = {}
    html[ :in ] = <<HTML
<div style="font:big">
 <a href=".." style="drop" rel="foo">link text</a>
 <img src=".." alt="foo" height="33" width="44" align="left"/>
</div>
HTML

    html[ :out ] = cut_atts( html[ :in ], 'style', 'align' )

    assert_transform( html, AttributeCleaner.new )
  end

  def cut_atts( html, *atts )
    atts.each do |att|
      html = html.gsub( / #{att}="[^"]+"/, '' )
    end
    html
  end

end
