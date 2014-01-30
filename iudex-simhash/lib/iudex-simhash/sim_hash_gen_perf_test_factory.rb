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

require 'iudex-simhash'
require 'iudex-simhash/factory_helper'

class SimHashGenPerfTestFactory
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

  Order = HTMLTreeFilter::Order

  import 'iudex.simhash.filters.SimHashGenPerfTest'

  def initialize
    UniMap.define_accessors
  end

  def perf_test

    # Initial parse
    map = content
    filter_chain.filter( map )

    SimHashGenPerfTest.new( map, simhash_generator )
  end

  def content
    map = UniMap.new

    html = File.read( File.join( File.dirname( __FILE__ ),  '..', '..',
                      'test', 'html', 'gentest.html' ) )

    map.source = HTMLUtils::source( html.to_java_bytes, "UTF-8" )
    map
  end

  def filter_chain
    filters = []
    filters << HTMLParseFilter.new( ContentKeys::SOURCE,
                                    nil, HTMLKeys::SOURCE_TREE )
    filters << TitleExtractor.new
    filters << TextCtrlWSFilter.new( ContentKeys::TITLE )

    tfc = TreeFilterChain.new( [ MetaSkipFilter.new,
                                 CharactersNormalizer.new,
                                 WordCounter.new,
                                 WordyCounter.new ] )

    filters << HTMLTreeFilter.new( HTMLKeys::SOURCE_TREE,
                                   tfc, Order::DEPTH_FIRST )

    FilterChain.new( "perf_test", filters )
  end

end
