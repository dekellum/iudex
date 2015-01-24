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

require 'iudex-html'
require 'iudex-html/factory_helper'

require 'iudex-filter/key_helper'

require 'gravitext-xmlprod/extensions'

module HTMLTestHelper

  include Gravitext::HTMap
  UniMap.define_accessors

  include Gravitext::XMLProd

  include Iudex::Filter::Core
  include Iudex::HTML::Filters::FactoryHelper

  import 'com.gravitext.xml.tree.TreeUtils'

  import 'iudex.html.HTML'
  import 'iudex.html.HTMLUtils'
  import 'iudex.html.tree.TreeFilterChain'
  import 'iudex.html.tree.TreeWalker'

  def parse( html, charset = "UTF-8" )
    HTMLUtils::parse( source( html, charset ) )
  end

  def parseFragment( html, charset = "UTF-8" )
    inner( HTMLUtils::parseFragment( source( html, charset ) ) )
  end

  def inner( tree )
    c = tree.children
    if ( c.size == 1 && c[0].element? )
      c[0]
    else
      tree
    end
  end

  def assert_doc( html, root )
    html = compress( html )
    assert_equal( html, root.to_xml )
  end

  def assert_fragment( html, root, remove_padding = false )
    assert_fragment_ws( compress( html ), root, remove_padding )
  end

  def assert_fragment_ws( html, root, remove_padding = false )
    html = html.gsub( /~+/, '' ) if remove_padding
    assert_equal( html, root.to_xml( :implied_ns => [ HTML::NS_XHTML ] ) )
  end

  def assert_transform( html, filter = nil, func = :walk_depth_first )
    tree = parseFragment( html[ :in ] )
    action = TreeWalker.send( func, filter, tree ) if func && filter
    assert_fragment( html[ :out ], tree, true )
    action
  end

  def source( html, charset = "UTF-8" )
    HTMLUtils::source( compress( html ).to_java_bytes, charset )
  end

  def compress( html )
    html.gsub( /\n\s*/, '' )
  end

  def filter_chain( filters, mode = :whole )
    pf = html_parse_filter( :source )
    pf.parse_as_fragment = true if mode == :fragment
    filters = Array( filters )
    filters.unshift( pf )
    FilterChain.new( "test", filters )
  end

  def content( html, charset = "UTF-8" )
    map = UniMap.new
    map.source = HTMLUtils::source( html.to_java_bytes, charset )
    map
  end

end
