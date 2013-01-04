#!/usr/bin/env jruby
# -*- coding: utf-8 -*-
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2013 David Kellum
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
require 'iudex-core'

class TestVisitURL < MiniTest::Unit::TestCase
  include Iudex::Core

# def setup; end
# def teardown end

  def test_normalize_basic

    sets = [ %w[ http://h.c/foo http://h.c/foo
                                http://h.c//foo
                                http://h.c/foo#anchor
                                HTTP://H.C:80/foo
                                HTTP://h.c/bar/../foo
                                http://h.c/./foo
                                http://h.c./foo
                                http://h.c/foo? ],

             %w[ http://h.c/    http://h.c ],

             %w[ http://127.0.0.1/ http://127.0.0.1:80 ],

             %w[ https://h.c/   httpS://h.c:443/
                                httpS://h.c:443 ],

             %w[ http://h.c/?x=a%26b http://h.c/?x=a%26b ],

             [ "http://h.c/foo", " \thttp://h.c/foo\n\r\t" ],
             [ "http://h.c/foo?q=a+b",   "http://h.c/foo?q=a+b" ],
             [ "http://h.c/foo?q=a%20b", "http://h.c/foo?q=a b",
                                         "http://h.c/foo?q=a  b",
                                         "HTTP://h.c/foo?q=a%20b#anchor",
                                         "http://h.c/foo?q=a\t b#anchor\t" ] ]

    sets.each do |tset|
      expected = tset.shift
      tset.each do |raw|
        assert_equal( expected, VisitURL.normalize( raw ).to_s )
      end
    end
  end

  def test_normalize_utf8

    sets = [ %w[ http://h.c/f%C5%8Do HTTP://h.c/fōo ] ]

    sets.each do |tset|
      expected = tset.shift
      tset.each do |raw|
        assert_equal( expected, VisitURL.normalize( raw ).to_s )
      end
    end
  end

  def test_bad_urls
    bads =   [ '',
               ' ',
               '.',
               ':',
               '\\',
               '\/' ] +
           %w[ bogus
               bogus:
               bogus:/
               bogus:/bar
               http
               http:
               http:/
               http://
               http:///
               http:///path/
               http://\[h/
               http://h\]/
               http://::/
               http://[:]/
               http://wonkie\biz
               http://wonkie/biz\ness
               https://h.c:-33/
               https://h.c:0/
               https://h.c:65537/ ]

    bads.each do |raw|
      begin
        flunk "[#{raw}] normalized to [#{VisitURL.normalize( raw )}]"
      rescue NativeException => e
        if e.cause.is_a?( VisitURL::SyntaxException )
          pass
        else
          raise e
        end
      end
    end

  end

  def test_ipv6
    # Demonstrate validity from a URI perspective, but likely want to
    # post-filter these.
    # http://www.ietf.org/rfc/rfc2732.txt
    sets = [
      %w[ http://[fedc:ba98:7654:3210:fedc:ba98:7654:3210]/index.html
          http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80/index.html ],
      %w[ http://[1080:0:0:0:8:800:200c:417a]/index.html ],
      %w[ http://[3ffe:2a00:100:7031::1]/
          http://[3ffe:2a00:100:7031::1] ],
      %w[ http://[1080::8:800:200c:417a]/foo ],
      %w[ http://[::]/ ], #FIXME: Unspecified or multicast
      %w[ http://[::192.9.5.5]/ipng ],
      %w[ http://[::ffff:129.144.52.38]/index.html
          http://[::FFFF:129.144.52.38]:80/index.html ],
      %w[ http://[2010:836b:4179::836b:4179]/
          http://[2010:836B:4179::836B:4179] ] ]

    sets.each do |tset|
      expected = tset.shift
      tset = [ expected ] if tset.empty? #identity test
      tset.each do |raw|
        assert_equal( expected, VisitURL.normalize( raw ).to_s )
      end
    end

  end

  def test_normalize_escape_case
    skip( "Escape normalizations not implemented" )

    sets = [ %w[ http://h.c/?x=a%3Ab http://h.c/?x=a%3ab ],
             %w[ http://h.c/%C2      http://h.c/%C2
                                     http://h.c/%c2 ],
             %w[ http://h.c/foo%20bar HTTP://h.c/%66oo%20bar ],
             %w[ http://h.c/a%5Bb%5D http://h.c/a[b] ] ]

    sets.each do |tset|
      expected = tset.shift
      tset.each do |raw|
        assert_equal( expected, VisitURL.normalize( raw ).to_s )
      end
    end
  end

  def test_normalize_idn
    skip( "IDN normalization not implemented" )

    sets = [ %w[ http://xn--bcher-kva.ch/ http://Bücher.ch ] ]

    sets.each do |tset|
      expected = tset.shift
      tset.each do |raw|
        assert_equal( expected, VisitURL.normalize( raw ).to_s )
      end
    end
  end

  def test_uhash
    h = VisitURL.normalize( "http://gravitext.com/" ).uhash
    assert_equal( "8dOml647JKxoA1vSNdi3WAK", h.to_s )

    h = VisitURL.normalize( "http://gravitext.com/x/y" ).uhash
    assert_equal( "0pRfQvGEzGRMQ-RgFbytf7l", h.to_s )
  end

  def test_domain_hash
    d = VisitURL.hash_domain( "gravitext.com" );
    assert_equal( "VdYKPM", d.to_s )

    d = VisitURL.hash_domain( "other.com" );
    assert_equal( "ZleSiQ", d.to_s )
  end

  def test_resolve

    sets = [ %w[ http://h.c/        http://h.c/foo ] << "",
             %w[ http://h.c/        http://h.c/    ] << "",
             %w[ http://h.c/        http://h.c/foo ] << " ",

             %w[ http://h.c/        http://h.c/foo      .   ],
             %w[ http://h.c/bar     http://h.c/foo     /bar ],
             %w[ http://h.c/bar     http://h.c/foo      bar ],
             %w[ http://h.c/bar     http://h.c/foo?q=1  bar ],
             %w[ http://h.c/bar     http://h.c/foo/x/y /bar ],
             %w[ http://h.c/foo/bar http://h.c/foo/x/y ../bar ],
             %w[ http://h.c/foo/bar http://h.c/foo/     bar ],

             %w[ http://h.c/a%20b/c%20d http://h.c/a%20b/f ] << "c d",

             %w[ http://h.c/bar?q=1     http://h.c/foo      bar?q=1 ],
             %w[ http://h.c/bar?q=1     http://h.c/foo/    /bar?q=1 ],
             %w[ http://h.c/bar?q=1     http://h.c/foo?x=2  bar?q=1 ],
             %w[ http://h.c/foo/bar?q=1 http://h.c/foo/     bar?q=1 ],
             %w[ http://h.c/foo/bar?q=1 http://h.c/foo/   ./bar?q=1 ] ]

    sets.each do |e,b,r|
      base = VisitURL.normalize( b )
      resolved = base.resolve( r )
      assert_equal( e, resolved.to_s, [ e,b,r ].inspect )
    end

  end

end
