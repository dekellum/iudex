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

  HTML_FRAG     = "<div>one<p>foo</p> two</div>"
  HTML_FRAG_OUT = "<div>one two</div>"

  class DropFilter
    include TreeFilter
    def filter( node )
      elm = node.as_element
      if( elm && elm.tag == HTML::P )
        TreeFilter::Action::DROP
      else
        TreeFilter::Action::CONTINUE
      end
    end
  end

  def test_drop
    tree = parse( HTML_FRAG )
    TreeWalker::walk_depth_first( DropFilter.new, tree )
    assert_xml( HTML_FRAG_OUT, tree )
  end

  def parse( html, charset="UTF-8" )
    comp_bytes = html.to_java_bytes
    HTMLUtils::parse( HTMLUtils::source( comp_bytes, charset ) )
  end

  def assert_xml( xml, root )
    assert_equal( xml,
      HTMLUtils::produceFragmentString( root, Indentor::COMPRESSED ) )
  end

end
