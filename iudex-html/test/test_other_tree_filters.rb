#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2010-2011 David Kellum
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

  def test_attribute_cleaner
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

  def test_empty_inline_remover

    hs = [ { :in  => "<div><b> keep </b></div>",
             :out => "<div><b> keep </b></div>" },

           { :in  => '<div><b><img src="keep"/></b></div>',
             :out => '<div><b><img src="keep"/></b></div>' },

           { :in  => "<div>first<span/></div>",
             :out => "<div>first~~~~~~~</div>" },

           { :in  => "<div>first<b> </b></div>",
             :out => "<div>first~~~ ~~~~</div>" },

           { :in  => "<div><b><span/></b>last</div>",
             :out => "<div>~~~~~~~~~~~~~~last</div>" },

           { :in  => "<div><b><span/> </b>last</div>",
             :out => "<div>~~~~~~~~~~ ~~~~last</div>" },

           { :in  => "<div><b> <br/> </b>last</div>",
             :out => "<div>~~~ <br/> ~~~~last</div>" } ]

    hs.each do |html|
      assert_transform( html, EmptyInlineRemover.new )
    end

  end

  def test_css_display_filter_pattern
    f = CSSDisplayFilter.new
    assert( f.has_display_none( 'display: none' ) )
    assert( f.has_display_none( '{display: none}' ) ) #lenient
    assert( f.has_display_none( 'other:foo; DISPLAY:NONE;' ) )

    refute( f.has_display_none( 'display: block' ) )
    refute( f.has_display_none( 'other-display: none' ) )
    refute( f.has_display_none( 'display: nonetheless' ) )
  end

  def test_css_display_filter
    html = {}
    html[ :in ] = <<HTML
<div>
 <b>keep</b>
 <div style="display:none"><b>drop</b> me</div>
</div>
HTML
    html[ :out ] = <<HTML
<div>
 <b>keep</b>
</div>
HTML
    assert_transform( html, CSSDisplayFilter.new )
  end

  def test_xmp_to_pre_converter
    html = { :in  => "<div><xmp> <i>keep</i> </xmp></div>",
             :out => "<div><pre> &lt;i>keep&lt;/i> </pre></div>" }

    assert_transform( html, XmpToPreConverter.new )
  end

  def cut_atts( html, *atts )
    atts.each do |att|
      html = html.gsub( / #{att}="[^"]+"/, '' )
    end
    html
  end

end
