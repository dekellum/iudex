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

class TestTreeWalker < MiniTest::Unit::TestCase
  import 'com.gravitext.xml.producer.Indentor'
  import 'iudex.html.HTML'
  import 'iudex.html.HTMLUtils'
  import 'iudex.html.tree.TreeFilter'
  import 'iudex.html.tree.TreeWalker'
  import 'iudex.html.tree.TreeFilterChain'

  Action = TreeFilter::Action

  DROP_HTML = {
    :in  => "<div>one<p>foo</p><br/> two</div>",
    :out => "<div>one~~~~~~~~~~<br/> two</div>" }
  # Note: ~~~ is padding removed in compare

  def test_drop
    [ :walk_depth_first, :walk_breadth_first ].each do |order|
      assert_transform( DROP_HTML, TagFilter.new( HTML::P, Action::DROP ), order )
    end
  end

  SKIP_HTML = {
    :in  => "<div>first<b>drop</b><span><b>not dropped</b></span></div>",
    :out => "<div>first~~~~~~~~~~~<span><b>not dropped</b></span></div>" }

  def test_skip
    chain = TreeFilterChain.new( [ TagFilter.new( HTML::SPAN, Action::SKIP ),
                                   TagFilter.new( HTML::B, Action::DROP ) ] )
    assert_transform( SKIP_HTML, chain, :walk_breadth_first )
  end

  class TagFilter
    include TreeFilter

    def initialize( tag, action )
      @tag = tag
      @action = action
    end

    def filter( node )
      elm = node.as_element
      if( elm && elm.tag == @tag )
        @action
      else
        Action::CONTINUE
      end
    end
  end

  def assert_transform( html, filter, func )
    tree = parse( html[ :in ] )
    TreeWalker.send( func, filter, tree )
    assert_xml( html[ :out ], tree )
  end

  def parse( html, charset="UTF-8" )
    comp_bytes = html.to_java_bytes
    HTMLUtils::parse( HTMLUtils::source( comp_bytes, charset ) )
  end

  def assert_xml( xml, root )
    xml = xml.gsub( /~+/, '' ) # Remove padding.
    assert_equal( xml,
      HTMLUtils::produceFragmentString( root, Indentor::COMPRESSED ) )
  end

end
