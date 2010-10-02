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
require 'iudex-html'

class TestHTMLParser < MiniTest::Unit::TestCase
  import 'iudex.html.HTMLUtils'
  import 'com.gravitext.xml.tree.TreeUtils'
  import 'com.gravitext.xml.producer.Indentor'

  HTML_META = <<HTML
<html xmlns="http://www.w3.org/1999/xhtml">
 <head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
  <title>Iūdex</title>
 </head>
 <body>
  <p>Iūdex test.</p>
 </body>
</html>
HTML

  def test_charset_same
    assert_xml( HTML_META, parse( HTML_META, "UTF-8" ) )
  end

  def test_charset_rerun
    assert_xml( HTML_META, parse( HTML_META, "ISO-8859-1" ) )
  end

  def test_charset_bogus
    alt = HTML_META.sub( /utf-8/, 'bogus' )
    assert_xml( alt, parse( alt, "UTF-8" ) )
  end

  HTML_SKIP_TAGS = <<HTML
<html xmlns="http://www.w3.org/1999/xhtml">
 <head>
  <style>style me</style>
 </head>
 <body>
  <unknown_empty/>
  <p>normal text.</p>
  <not_empty><p>foo</p><br/></not_empty>
  <nostyle><p>foo</p><br/></nostyle>
 </body>
</html>
HTML

  HTML_SKIP_TAGS_SKIPPED = <<HTML
<html xmlns="http://www.w3.org/1999/xhtml">
 <head/>
 <body>
  <p>normal text.</p>
 </body>
</html>
HTML

  def test_skip_tags
    assert_xml( HTML_SKIP_TAGS_SKIPPED, parse( HTML_SKIP_TAGS, "ISO-8859-1" ) )
  end

  def parse( html, charset )
    comp_bytes = html.gsub( /\n\s*/, '' ).to_java_bytes
    HTMLUtils::parse( HTMLUtils::source( comp_bytes, charset ) )
  end

  def assert_xml( xml, root )
    assert_equal( xml, TreeUtils::produceString( root, Indentor::PRETTY ) )
  end

end
