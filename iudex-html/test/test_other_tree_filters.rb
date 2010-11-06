#!/usr/bin/env jruby
# -*- coding: utf-8 -*-
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
require 'iudex-html'

class TestOtherTreeFilters < MiniTest::Unit::TestCase
  include Iudex::HTML::Tree
  include Iudex::HTML::Tree::Filters

  import 'com.gravitext.xml.producer.Indentor'
  import 'iudex.html.HTMLUtils'
  import 'iudex.html.tree.TreeWalker'

  def test_non_html_atts_dropped
    # Bogus is dropped already by parser
    html = {}
    html[ :in ] = <<HTML
<div bogus="not html">
 <p>Iūdex test.</p>
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

  def assert_transform( html, filter = nil, func = :walk_depth_first )
    tree = parse( html[ :in ] )
    action = TreeWalker.send( func, filter, tree ) if func && filter
    assert_xml( html[ :out ], tree )
    action
  end

  def parse( html, charset="UTF-8" )
    comp_bytes = html.gsub( /\n\s*/, '' ).to_java_bytes
    tree = HTMLUtils::parseFragment( HTMLUtils::source( comp_bytes, charset ) )
    c = tree.children
    if ( c.size == 1 && c[0].element? )
      c[0]
    else
      tree
    end
  end

  def assert_xml( xml, root )
    xml = xml.gsub( /\n\s*/, '' )
    assert_equal( xml,
      HTMLUtils::produceFragmentString( root, Indentor::COMPRESSED ) )
  end

end
