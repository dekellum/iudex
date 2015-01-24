#!/usr/bin/env jruby
# -*- coding: utf-8 -*-
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

class TestHTMLParser < MiniTest::Unit::TestCase
  include HTMLTestHelper
  import 'iudex.util.Charsets'

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

  def test_charset_missing
    alt = HTML_META.sub( /; charset=utf-8/, '' )
    assert_doc( alt, parse( alt, "UTF-8" ) )
  end

  def test_meta_charset_rerun
    alt = HTML_META.sub( /<meta .*\/>/, '<meta charset="utf-8"/>' )
    assert_doc( alt, parse( alt, "ISO-8859-1" ) )
  end

  def test_meta_charset_conflict
    alt = HTML_META.sub( /(<head>)/, '<head><meta charset="ISO-8859-1"/>' )
    assert_doc( alt, parse( alt, "ISO-8859-1" ) )
  end

  def test_meta_charset_conflict_2
    alt = HTML_META.sub( /utf-8/, 'latin1' )
    alt = HTML_META.sub( /(<\/head>)/, '<meta charset="UTF-8"/></head>' )
    assert_doc( alt, parse( alt, "ISO-8859-1" ) )
  end

  HTML_META_2 = <<HTML
<!DOCTYPE html>
<head>
<title>Page with skipped head tags</title>
<meta name="description" content="">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<script type="text/javascript" src="../js/swfobject.js"></script>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
</head>
<body>
 <p>Iudex test.</p>
</body>
</html>
HTML

  def test_charset_conflict_3
    src = source( HTML_META_2, "Windows-1252" )
    src.set_default_encoding( Charsets::UTF_8, 0.10 )
    assert( HTMLUtils::parse( src ) )
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

  def test_attr_duplicates
    input = <<-HTML
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
  <head/>
  <body>
    <p class="foo" class="bar">hello</p>
  </body>
</html>
HTML
    output = <<-HTML
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
  <head/>
  <body>
    <p class="bar">hello</p>
  </body>
</html>
HTML
    assert_doc( output, parse( input ) )
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

  # Neko doesn't ban/reorder blocks in inline elements.
  def test_inline_nest
    html = { :in  => "<div><i>begin <p>block</p> end.</i></div>",
             :out => "<div><i>begin <p>block</p> end.</i></div>" }
    tree = parseFragment( html[ :in ] )
    assert_fragment( html[ :out ], tree )
  end

  import 'iudex.html.neko.NekoHTMLParser'

  # Neko yields attributes with empty localName, given this invalid
  # input (#8)
  def test_invalid_attribute
    html = { :in  => '<div><img alt=""wns : next class="artwork" /></div>',
             :out => '<div><img alt="" wns="" next="" class="artwork"/></div>' }

    parser = NekoHTMLParser.new
    parser.parse_as_fragment = true
    parser.skip_banned = false # required to reproduce empty localName

    tree = inner( parser.parse( source( html[ :in ], "UTF-8" ) ) )
    assert_fragment( html[ :out ], tree )
  end

end
