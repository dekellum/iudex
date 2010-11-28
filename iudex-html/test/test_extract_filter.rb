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

class TestExtractFilter < MiniTest::Unit::TestCase
  include HTMLTestHelper

  include Iudex::HTML::Filters
  include Iudex::HTML::Tree::Filters
  Order = HTMLTreeFilter::Order

  def test_doc
    html = <<HTML
<div>
 <p>Short junk</p>
 <br/>
 <hr/>
 <p>A more <i>substantive </i>paragraph.</p>
</div>
HTML
    map = content( html )
    fc = [ HTMLTreeFilter.new( :source_tree.to_k,
                               WordCounter.new, Order::DEPTH_FIRST ),
           ExtractFilter.new( [ :source_tree.to_k ] ) ]
    chain = filter_chain( fc, :fragment )
    assert( chain.filter( map ) )
    assert_equal( "A more substantive paragraph", map.extract )
  end

end
