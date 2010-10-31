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
require 'iudex-simhash'
require 'iudex-simhash/factory_helper'

class TestSimhashGenerator < MiniTest::Unit::TestCase
  include Gravitext::HTMap
  include Iudex::Core
  include Iudex::Core::Filters
  include Iudex::HTML
  include Iudex::HTML::Filters
  include Iudex::HTML::Tree
  include Iudex::HTML::Tree::Filters
  include Iudex::Filter::Core
  include Iudex::SimHash::Filters

  include Iudex::SimHash::Filters::FactoryHelper

  import 'iudex.html.HTMLUtils'

  UniMap.define_accessors

  Order = HTMLTreeFilter::Order

  def test_default_stopwords
    stopwords = simhash_stopwords
    assert( stopwords.contains( to_char_buffer( 'or' ) ) )
    #FIXME: String?
  end

  def to_char_buffer( s )
    Java::java.nio.CharBuffer.wrap( s )
  end

  def test_generate
    html = <<HTML
<html>
 <head>
  <title>Title</title>
 </head>
 <body>
  <p>We are talking about the same thing here.</p>
  <p>Really this is the same exact thing I was telling you last time.</p>
  <p>cruft</p> <!-- Ignored by default 0.3 wordy ratio -->
 </body>
</html>
HTML

    map = content( html )
    assert( filter_chain.filter( map ) )
    assert_equal( 'Title', map.title.to_s )
    assert_equal( 'eaa415092440bf4e', hex( map.simhash ) )

    html.gsub!( /the/, "\t" )       # Removing stop words doesn't matter
    html.gsub!( /cruft/, "xcruft" ) # cruft by any other name...
    map = content( html )
    assert( filter_chain.filter( map ) )
    assert_equal( 'eaa415092440bf4e', hex( map.simhash ) )
  end

  def content( html, charset = "UTF-8" )
    map = UniMap.new
    map.content = HTMLUtils::source( html.to_java_bytes, "UTF-8" )
    map
  end

  def filter_chain
    filters = []
    filters << HTMLParseFilter.new( ContentKeys::CONTENT,
                                    nil, HTMLKeys::CONTENT_TREE )
    filters << TitleExtractor.new
    filters << TextCtrlWSFilter.new( ContentKeys::TITLE )

    tfc = TreeFilterChain.new( [ MetaSkipFilter.new,
                                 CharactersNormalizer.new,
                                 WordCounter.new,
                                 WordyCounter.new ] )

    filters << HTMLTreeFilter.new( HTMLKeys::CONTENT_TREE,
                                   tfc, Order::DEPTH_FIRST )

    filters << simhash_generator

    FilterChain.new( "test", filters )
  end

  def hex( l )
    Java::java.lang.Long::toHexString( l )
  end

end
