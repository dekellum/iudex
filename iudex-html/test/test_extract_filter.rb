#!/usr/bin/env jruby
# -*- coding: utf-8 -*-
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

class TestExtractFilter < MiniTest::Unit::TestCase
  include HTMLTestHelper
  include Iudex::HTML::Filters
  include Iudex::Filter::KeyHelper

  include Iudex::HTML::Tree::Filters
  Order = HTMLTreeFilter::Order

  def test_single_tree
    htests = [ [ nil,                    nil ],
               [ '<div></div>',          nil ],
               [ '<div>too short</div>', nil ],
               [ <<-'HTML', nil ],
                 <div>
                   <p>too short</p>
                 </div>
                 HTML
               [ <<-'HTML', nil ],
                 <div>a1 a2<br/>
                      a3 a4</div>
                 HTML
               [ <<-'HTML', "a1 a2 a3 a4" ],
                 <div>a1 a2 a3 a4</div>
                 HTML
               [ <<-'HTML', "a1 a2 a3 a4" ],
                 <div>
                   <p>a1 a2 a3 a4</p>
                 </div>
                 HTML
               [ <<-'HTML', "a1 a2 a3 a4" ],
                 <div>a1 a2 a3 a4<br/>
                      not part of extract</div>
                 HTML
               [ <<-'HTML', "a1 a2 a3 a4" ],
                 <div>not<br/>
                      a1 a2 a3 a4
                      </div>
                 HTML
               [ <<-'HTML', "A more substantive paragraph." ],
                 <div>
                   <p>Short junk</p>
                   <hr/>
                   <p>A more <i>substantive </i>paragraph.</p>
                   <p>A similarly <i>substantive </i>paragraph.</p>
                 </div>
                 HTML
               [ <<-'HTML', "A more substantive paragraph." ],
                 <i>
                   <p>Short junk</p>
                   <i>
                     <hr/>
                     <p>A more <i>substantive </i>paragraph.</p>
                   </i>
                   <p>A similarly <i>substantive </i>paragraph.</p>
                 </i>
                 HTML
               [ <<-'HTML', "a1 a2 a3 a4 a5 a6 a7 a8" ],
                 <div>
                   <p>a1 a2 a3</p>
                   <p>a1 a2 a3 a4 a5</p>
                   <p>a1 a2 a3 a4 a5 a6</p>
                   <p>a1 a2 a3 a4 a5 a6 a7</p>
                   <p>a1 a2 a3 a4 a5 a6 a7 a8</p>
                   <p>a1 a2 a3 a4 a5 a6 a7 a8 a9</p>
                 </div>
                 HTML
               [ <<-'HTML', "a1 a2 a3 a4 a5 a6 a7 a8" ],
                 <div>
                   <p>a1 a2 a3 a4 a5 a6 a7</p>
                   <p>a1 a2 a3</p>
                   <p>a1 a2 a3 a4 a5</p>
                   <p>a1 a2 a3 a4 a5 a6</p>
                   <div>
                      <p>a1 a2 a3 a4 a5 a6 a7 a8</p>
                         a1 a2 a3 a4 a5 a6 a7 a8 a9
                   </div>
                 </div>
                 HTML
               [ <<-'HTML', "a1 a2 a3 a4 a5 a6 a7 a8" ],
                 <div>
                   <p>a1 a2 a3</p>
                   <p>a1 a2 a3 a4 a5</p>
                   <p>a1 a2 a3 a4 a5 a6</p>
                   <p>a1 a2 a3 a4 a5 a6 a7</p>
                   <div>
                        a1 a2 a3 a4 a5 a6 a7 a8
                     <p>a1 a2 a3 a4 a5 a6 a7 a8 a9</p>
                   </div>
                 </div>
                 HTML
             ]

    htests.each do | html, exp_extract |
      map = ( content( html ) if html ) || UniMap.new
      tfc = TreeFilterChain.new( [ CharactersNormalizer.new,
                                   WordCounter.new ] )
      fc = [ HTMLTreeFilter.new( :source_tree.to_k, tfc, Order::DEPTH_FIRST ),
             ExtractFilter.new( [ :source_tree.to_k ] ) ]
      chain = filter_chain( fc, :fragment )
      assert( chain.filter( map ) )
      assert_equal( exp_extract, map.extract && map.extract.to_s,
                    "from:\n" + html.to_s )
    end
  end

  def test_multi_tree
    htests = [ [ nil, nil, nil ],
               [ <<-'HTML1', <<-'HTML2', 'a1 a2 a3 a4' ],
                  <div>too short</div>
                 HTML1
                  <div>a1 a2 a3 a4</div>
                 HTML2
               [ <<-'HTML1', <<-'HTML2', 'a1 a2 a3 a4' ],
                  <div>a1 a2 a3 a4</div>
                 HTML1
                  <div>too short</div>
                 HTML2
               [ <<-'HTML1', <<-'HTML2', 'a1 a2 a3 a4 a5 a6 a7 a8' ],
                  <div>a1 a2 a3 a4 a5 a6 a7 a8</div>
                 HTML1
                  <div>a1 a2 a3 a4 a5 a6 a7 a8 a9</div>
                 HTML2
             ]

    htests.each do | summary, content, exp_extract |
      map = UniMap.new
      map.summary = summary
      map.content = content

      filters = [ html_clean_filters( :summary ),
                  html_clean_filters( :content ),
                  ExtractFilter.new( keys( :summary_tree, :content_tree ) ) ]

      chain = FilterChain.new( "test", filters.flatten )
      assert( chain.filter( map ) )
      assert_equal( exp_extract, map.extract && map.extract.to_s,
                    "summary: #{summary}\n" +
                    "content: #{content}" )
    end

  end

end
