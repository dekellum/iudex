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

class TestHTMLParser < MiniTest::Unit::TestCase
  include HTMLTestHelper

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
    assert_doc( HTML_META, parse( HTML_META, "UTF-8" ) )
  end

  def test_charset_rerun
    assert_doc( HTML_META, parse( HTML_META, "ISO-8859-1" ) )
  end

  def test_charset_bogus
    alt = HTML_META.sub( /utf-8/, 'bogus' )
    assert_doc( alt, parse( alt, "UTF-8" ) )
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
    assert_doc( HTML_SKIP_TAGS_SKIPPED, parse( HTML_SKIP_TAGS, "ISO-8859-1" ) )
  end

  HTML_OUTSIDE = <<HTML
before
<html xmlns="http://www.w3.org/1999/xhtml">
 <head/>
 <body>
  <p>normal text.</p>
 </body>
</html>
after
HTML

  HTML_INSIDE = <<HTML
<html xmlns="http://www.w3.org/1999/xhtml">
 <head/>
 <body>before
  <p>normal text.</p>after</body>
</html>
HTML

  def test_outer_text
    assert_doc( HTML_INSIDE, parse( HTML_OUTSIDE, "ISO-8859-1" ) )
  end

  HTML_FRAG = {
    :in  =>      "one<p>two</p><br/> three",
    :out => "<div>one<p>two</p><br/> three</div>" }

  def test_parse_fragment
    tree = parseFragment( HTML_FRAG[ :in ] )
    assert_fragment( HTML_FRAG[ :out ], tree )
  end

  HTML_CDATA = {
    :in  => "<p><![CDATA[two]]></p>",
    :out => "<p/>" }
  # By default (incl HTML browsers) CDATA sections are dropped.

  def test_cdata
    tree = parseFragment( HTML_CDATA[ :in ] )
    assert_fragment( HTML_CDATA[ :out ], tree )
  end

end
