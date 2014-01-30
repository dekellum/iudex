#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2014 David Kellum
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

class TestRedirectHandler < MiniTest::Unit::TestCase
  include Iudex::HTTP
  include Iudex::Core
  include Iudex::Core::Filters
  include Gravitext::HTMap

  UniMap.define_accessors

  def test_first_redirect
    order = new_order
    redirect( order )
    do_filter( order )

    assert_equal( "http://www/1", order.url.to_s )
    assert_equal( 301, order.status )
    assert_equal( 1.0, order.priority )

    order = revisit( order )
    assert_nil( order.status )
    assert_equal( 1.5, order.priority )

    assert_equal( "http://www/2", order.url.to_s )
    assert_equal( "http://www/1", order.last.url.to_s )

    assert_equal( "http://www/1", order.referer.url.to_s )
    assert_equal( "http://www/2", order.referer.referent.url.to_s )
  end

  def test_second_redirect
    order = new_order
    redirect( order, 301, 2 )
    do_filter( order )

    order = revisit( order )
    redirect( order, 302, 3 )
    do_filter( order )

    order = revisit( order )

    assert_equal( 2.0, order.priority )

    assert_equal( "http://www/3", order.url.to_s )
    assert_equal( "http://www/2", order.last.url.to_s )
    assert_equal( "http://www/1", order.referer.url.to_s )
    assert_equal( "http://www/3", order.referer.referent.url.to_s )
  end

  def test_third_redirect
    order = new_order
    redirect( order, 301, 2 )
    do_filter( order )

    order = revisit( order )
    redirect( order, 302, 3 )
    do_filter( order )

    order = revisit( order )
    redirect( order, 303, 4 )
    do_filter( order )

    order = revisit( order )

    assert_equal( 2.5, order.priority )

    assert_equal( "http://www/4", order.url.to_s )
    assert_equal( "http://www/3", order.last.url.to_s )
    assert_equal( "http://www/2", order.last.last.url.to_s )
    assert_equal( "http://www/1", order.referer.url.to_s )
    assert_equal( "http://www/4", order.referer.referent.url.to_s )
  end

  def test_self_redirect
    order = new_order
    redirect( order, 307, 1 )
    do_filter( order )

    assert_equal( "http://www/1", order.url.to_s )
    assert_equal( HTTPSession::REDIRECT_LOOP, order.status )
    assert_equal( 1.0, order.priority )
    assert_nil( order.revisit_order )
  end

  def test_missing_location
    order = new_order
    order.status = 307
    do_filter( order )

    assert_equal( "http://www/1", order.url.to_s )
    assert_equal( HTTPSession::MISSING_REDIRECT_LOCATION, order.status )
    assert_equal( 1.0, order.priority )
    assert_nil( order.revisit_order )
  end

  def test_redirect_loop
    order = new_order
    redirect( order, 301, 2 )
    do_filter( order )

    order = revisit( order )
    redirect( order, 302, 3 )
    do_filter( order )

    order = revisit( order )
    redirect( order, 303, 1 ) #Eat our tail
    do_filter( order )

    assert_equal( "http://www/3", order.url.to_s )
    assert_equal( HTTPSession::REDIRECT_LOOP, order.status )
    assert_nil( order.revisit_order )
  end

  def test_max_path
    order = new_order
    (2..5).each do |i|
      redirect( order, 302, i )
      do_filter( order, 3 )
      order = revisit( order ) || break
    end

    assert_equal( "http://www/3", order.url.to_s )
    assert_equal( HTTPSession::MAX_REDIRECTS_EXCEEDED, order.status )
    assert_nil( order.revisit_order )
  end

  def do_filter( order, max_path = 4 )
    orig_url = order.url
    handler = RedirectHandler.new
    handler.max_path = max_path
    handler.filter( order )
  end

  def revisit( order )
    order.remove( ContentKeys::REVISIT_ORDER )
  end

  def new_order( i = 1 )
    UniMap.new.tap do |o|
      o.url = VisitURL.normalize( "http://www/#{i}" )
      o.priority = 1.0
    end
  end

  def redirect( o, s = 301, r = 2 )
    o.status = 301
    o.response_headers = [ Header.new( "Location", "http://WWW/#{r}" ) ]
  end

end
