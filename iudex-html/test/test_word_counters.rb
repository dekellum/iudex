#!/usr/bin/env jruby
# -*- coding: utf-8 -*-
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2010-2014 David Kellum
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

class TestWordCounters < MiniTest::Unit::TestCase
  include HTMLTestHelper
  include Iudex::HTML::Tree
  include Iudex::HTML::Tree::Filters

  def test_counts
    tset = [ [ "",                                            0, 0 ],
             [ "<div><span> </span></div>",                   0, 0 ],

             [ "a b",                                         2, 2 ],
             [ "<div>a b</div>",                              2, 2 ],
             [ "<div><span>a b</span></div>",                 2, 2 ],
             [ "<div><span>a b</span><span>c d</span></div>", 4, 4 ],

             [ "<div>a b <span><a name='foo'>c</a> d</span> e f</div>", 6, 6 ],

             [ "<div><a href='foo'>a b</a></div>",
               2, 2 * 0.25 ],

             [ "<div><div>a b</div><div>c d</div></div>",
               4,       ( 2*2 + 2*2 ) / 4.0 ],

             [ "<div><div>a b</div><div>c d</div><div> </div></div>",
               4,       ( 2*2 + 2*2 + 0*0 ) / 4.0 ],

             [ "<div>a <div>a b</div><div>c d</div></div>",
               5, 1.0 + ( 2*2 + 2*2 ) / 5.0 ],

             [ "<div>a <div>a b c</div><div>c d</div></div>",
               6, 1.0 + ( 3*3 + 2*2 ) / 6.0 ],

             [ "<div><p>a b</p><p>c d</p></div><div>e f g</div>",
               7,       ( 2*4 + 3*3 ) / 7.0 ] ]

    chain = TreeFilterChain.new( [ WordCounter.new, WordyCounter.new ] )

    tset.each do |html, word_count, wordiness|
      tree = parse( html )
      TreeWalker::walk_depth_first( chain, tree )

      assert_equal( word_count,
                    tree.get( HTMLTreeKeys::WORD_COUNT ),
                    "word_count for: " + html )

      assert_in_delta( wordiness,
                       tree.get( HTMLTreeKeys::WORDINESS ),
                       1e-4,
                       " wordiness for: " + html )
    end
  end

  def test_doc
    html = <<HTML
<html xmlns="http://www.w3.org/1999/xhtml">
 <head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
  <title>Iūdex</title>
  <style>style</style>
 </head>
 <body>
  <p>Iūdex test.</p>
 </body>
</html>
HTML
    tree = parse( html, "UTF-8" )
    chain = TreeFilterChain.new( [ MetaSkipFilter.new,
                                   WordCounter.new,
                                   WordyCounter.new ] )
    TreeWalker::walk_depth_first( chain, tree )
    assert_equal( 2, tree.get( HTMLTreeKeys::WORD_COUNT ) );
    assert_equal( 2, tree.get( HTMLTreeKeys::WORDINESS ) );
  end

end
