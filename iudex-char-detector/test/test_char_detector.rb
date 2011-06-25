#!/usr/bin/env jruby
# -*- coding: utf-8 -*-
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2011 David Kellum
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
require 'iudex-char-detector'

class TestCharDetector < MiniTest::Unit::TestCase
  include Gravitext::HTMap
  UniMap.define_accessors

  include Iudex::Core
  include Iudex::CharDetector

  import 'java.nio.ByteBuffer'
  import 'java.nio.charset.Charset'

  SHORT_HTML = <<HTML
<html>
 <head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
 </head>
 <body>
  <p>¿De donde eres tú?</p>
 </body>
</html>
HTML

  def test_html_utf8_as_default
    map = detect_from( SHORT_HTML, "UTF-8" )
    assert_encoding( map.source, "UTF-8", 0.70 )
  end

  def test_html_utf8_wrong_default
    map = detect_from( SHORT_HTML, "UTF-8", "ISO-8859-1" )
    assert_encoding( map.source, "UTF-8", 0.70 )
  end

  def test_html_iso_as_default
    map = detect_from( SHORT_HTML, "ISO-8859-1" )
    assert_encoding( map.source, "ISO-8859-1", 0.40 )
  end

  def test_html_iso_wrong_default
    map = detect_from( SHORT_HTML, "ISO-8859-1", "UTF-8" )
    assert_encoding( map.source, "ISO-8859-1", 0.40 )
  end

  def test_html_iso_from_windows
    map = detect_from( SHORT_HTML, "windows-1252" )
    assert_encoding( map.source, "ISO-8859-1", 0.40 )
  end

  def test_windows_default
    map = detect_from( '“¿De donde eres tú?”', "windows-1252" )
    assert_encoding( map.source, "windows-1252", 0.90 )
  end

  def test_windows_wrong_default
    map = detect_from( '“¿De donde eres tú?”', "windows-1252", "UTF-8" )
    assert_encoding( map.source, "windows-1252", 0.90 )
  end

  def test_mojibaked_utf8
    map = detect_from( 'âquotedâ€', "UTF-8" )
    assert_encoding( map.source, "UTF-8", 0.99 )
  end

  def detect_from( bytes, enc, claimed_enc = nil )
    map = content( encode_as( bytes, enc ), claimed_enc || enc )
    assert( CharDetectFilter.new.filter( map ) )
    map
  end

  def assert_encoding( source, enc, min_confidence = 0.10 )
    assert_equal( enc, source.default_encoding.name )
    assert_operator( source.encoding_confidence, :>=, min_confidence )
  end

  def encode_as( bytes, encoding )
    if encoding == "UTF-8"
      bytes
    else
      bytes = bytes.to_java_bytes if bytes.respond_to?( :to_java_bytes )
      Java::java.lang.String.new( bytes, "UTF-8" ).bytes( encoding )
    end
  end

  def content( html, charset = "UTF-8" )
    map = UniMap.new
    html = html.to_java_bytes if html.respond_to?( :to_java_bytes )
    map.source = ContentSource.new( ByteBuffer::wrap( html ) )
    map.source.default_encoding = Charset::lookup( charset )
    map
  end

end
