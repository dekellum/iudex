#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2010-2015 David Kellum
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

class TestTreeWalker < MiniTest::Unit::TestCase
  include HTMLTestHelper

  import 'iudex.html.tree.TreeFilter'
  Action = TreeFilter::Action

  DROP_HTML = {
    :in  => "<div>one<p>foo</p><br/> two</div>",
    :out => "<div>one~~~~~~~~~~<br/> two</div>" }
  # Note: ~~~ is padding removed in compare

  def test_drop
    filter = TagFilter.new( HTML::P, Action::DROP )
    [ :walk_depth_first, :walk_breadth_first ].each do |order|
      assert_transform( DROP_HTML, filter, order )
    end
  end

  SKIP_HTML = {
    :in  => "<div>first<b>drop</b><span><b>not dropped</b></span></div>",
    :out => "<div>first~~~~~~~~~~~<span><b>not dropped</b></span></div>" }

  def test_skip
    chain = TreeFilterChain.new( [ TagFilter.new( HTML::SPAN, Action::SKIP ),
                                   TagFilter.new( HTML::B,    Action::DROP ) ] )
    assert_transform( SKIP_HTML, chain, :walk_breadth_first )
  end

  TERM_HTML = {
    :in  => "<div><span>first</span><b>term</b><span><b>not</b></span></div>",
    :out => "<div>~~~~~~~~~~~~~~~~~~<b>term</b><span><b>not</b></span></div>" }

  def test_terminate
    chain = TreeFilterChain.new( [ TagFilter.new( HTML::B, Action::TERMINATE ),
                                   TagFilter.new( HTML::SPAN, Action::DROP ) ] )
    [ :walk_depth_first, :walk_breadth_first ].each do |order|
      assert_equal( Action::TERMINATE,
                    assert_transform( TERM_HTML, chain, order ) )
    end
  end

  FOLD_HTML = {
    :in  => "<div>first <b>drop</b> <span> remain <b>drop</b> </span> </div>",
    :out => "<div>first ~~~~~~~~~~~ ~~~~~~ remain ~~~~~~~~~~~ ~~~~~~~ </div>" }

  def test_fold
    chain = TreeFilterChain.new( [ TagFilter.new( HTML::SPAN, Action::FOLD ),
                                   TagFilter.new( HTML::B,    Action::DROP ) ] )

    [ :walk_breadth_first, :walk_depth_first ].each do |order|
      assert_transform( FOLD_HTML, chain, order )
    end
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

end
