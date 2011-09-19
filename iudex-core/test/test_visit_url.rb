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

             %w[ http://h.c/?x=a%26b http://h.c/?x=a%26b ],

             [ "http://h.c/foo", " \thttp://h.c/foo\n\r\t" ],
             [ "http://h.c/foo?q=a+b",   "http://h.c/foo?q=a+b" ],
             [ "http://h.c/foo?q=a%20b", "http://h.c/foo?q=a b",
                                         "http://h.c/foo?q=a  b",
                                         "HTTP://h.c/foo?q=a%20b#anchor",
                                         "http://h.c/foo?q=a\t b#anchor\t" ] ]

    sets.each do |tset|
      expected = VisitURL.normalize( tset.shift )
      tset.each do |raw|
        assert_equal( expected.to_s, VisitURL.normalize( raw ).to_s )
      end
    end
  end

  def test_normalize_utf8

    sets = [ %w[ http://h.c/f%C5%8Do HTTP://h.c/fōo ] ]

    sets.each do |tset|
      expected = VisitURL.normalize( tset.shift )
      tset.each do |raw|
        assert_equal( expected.to_s, VisitURL.normalize( raw ).to_s )
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
      expected = VisitURL.normalize( tset.shift )
      tset.each do |raw|
        assert_equal( expected.to_s, VisitURL.normalize( raw ).to_s )
      end
    end
  end

  def test_normalize_idn
    skip( "IDN normalization not implemented" )

    sets = [ %w[ http://xn--bcher-kva.ch/ http://Bücher.ch ] ]

    sets.each do |tset|
      expected = VisitURL.normalize( tset.shift )
      tset.each do |raw|
        assert_equal( expected.to_s, VisitURL.normalize( raw ).to_s )
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
      expected = VisitURL.normalize( e )
      base = VisitURL.normalize( b )
      resolved = base.resolve( r )

      assert_equal( expected.to_s, resolved.to_s, [ e,b,r ].inspect )
    end

  end

end
