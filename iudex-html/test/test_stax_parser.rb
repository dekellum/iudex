#!/usr/bin/env jruby
# -*- coding: utf-8 -*-
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2012 David Kellum
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

class TestStAXParser < MiniTest::Unit::TestCase
  include HTMLTestHelper

  import 'javax.xml.stream.XMLStreamException'

  HTML_FULL = <<HTML
<html xmlns="http://www.w3.org/1999/xhtml">
 <head>
  <title>Iūdex</title>
 </head>
 <body>
  <p>Iūdex test.</p>
  <n:bogus xmlns:n="bogo">truely</n:bogus>
 </body>
</html>
HTML

  def test_with_non_html
    root = parse( HTML_FULL, "UTF-8" )
    assert( root.find( 'body' ).find( 'bogus' ).tag.banned? )
    assert_doc( HTML_FULL, root )
  end

  HTML_CDATA = {
    :in  => "<p><![CDATA[two]]></p>",
    :out => "<p>two</p>" }
  # Note: HTML parsers drop this instead.

  def test_cdata
    tree = parse( HTML_CDATA[ :in ] )
    assert_fragment( HTML_CDATA[ :out ], tree )
  end

  def test_inline_nest
    html = { :in  => "<div><i>begin <p>block</p> end.</i></div>",
             :out => "<div><i>begin <p>block</p> end.</i></div>" }
    tree = parse( html[ :in ] )
    assert_fragment( html[ :out ], tree )
  end

  def test_invalid_error
    assert_raises( XMLStreamException ) do
      Iudex::HTML::Tree.parse( "" )
    end

    assert_raises( XMLStreamException ) do
      Iudex::HTML::Tree.parse( "<doc><open></doc>" )
    end
  end

  # Helper overrrides
  def parse( html, charset = "UTF-8" )
    Iudex::HTML::Tree.parse( compress( html ) )
  end

end
