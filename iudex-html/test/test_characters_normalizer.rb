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
require 'iudex-html'

class TestCharactersNormalizer < MiniTest::Unit::TestCase
  include Iudex::HTML
  include Iudex::HTML::Filters
  include Iudex::HTML::Tree
  include Iudex::HTML::Tree::Filters
  include Gravitext::HTMap
  include Iudex::Core
  include Iudex::Filter::Core

  import 'com.gravitext.xml.producer.Indentor'
  import 'iudex.html.HTMLUtils'

  UniMap.define_accessors

  Order = HTMLTreeFilter::Order

  def test_simple_block
    # Note: '~' is padding removed in compare

    html = { :in  => "<p> x  y </p>",
             :out => "<p>~x ~y~</p>" }

    assert_transform( html )
  end

  def test_simple_inline
    html = { :in  => "<i> x  y </i>",
             :out => "<i> x ~y </i>" }

    assert_transform( html )
  end

  def test_mixed_inline
    html = { :in  => "<p> x  y <i>z </i> </p>",
             :out => "<p>~x ~y <i>z </i>~</p>" }

    assert_transform( html )
  end

  def test_empty
    html = { :in  => "<div><p> </p>  <p>foo</p> </div>",
             :out => "<div><p/>~~~~~~<p>foo</p>~</div>" }

    assert_transform( html )
  end

  def test_pre
    html = { :in  => "<div> x <pre>  \0x\n <a>  y </a></pre> </div>",
             :out => "<div>~x~<pre>  ~ x\n <a>  y </a></pre>~</div>" }

    assert_transform( html )
  end

  def assert_transform( html )
    [ Order::BREADTH_FIRST, Order::DEPTH_FIRST ].each do |order|
      map = content( html[ :in ] )
      tfc = TreeFilterChain.new( [ CharactersNormalizer.new ] )
      tf = HTMLTreeFilter.new( HTMLKeys::CONTENT_TREE, tfc, order )
      chain = filter_chain( tf  )
      assert( chain.filter( map ) )
      assert_xml( html[ :out ], inner( map.content_tree ) )
    end
  end

  def content( html, charset = "UTF-8" )
    map = UniMap.new
    map.content = HTMLUtils::source( html.to_java_bytes, "UTF-8" )
    map
  end

  def inner( tree )
    c = tree.children
    if ( c.size == 1 && c[0].element? )
      c[0]
    else
      tree
    end
  end

  def filter_chain( *filters )
    pf = HTMLParseFilter.new( ContentKeys::CONTENT,
                              nil, HTMLKeys::CONTENT_TREE )
    pf.parse_as_fragment = true
    filters.unshift( pf )
    FilterChain.new( "test", filters )
  end

  def assert_xml( xml, root )
    xml = xml.gsub( /~+/, '' ) # Remove padding.
    assert_equal( xml,
      HTMLUtils::produceFragmentString( root, Indentor::COMPRESSED ) )
  end

end
