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

require 'iudex-filter'
require 'iudex-filter/proc_filter'

class TestSubListFilter < MiniTest::Unit::TestCase
  include Iudex::Filter
  include Iudex::Filter::Core
  include Gravitext::HTMap

  SLIST = UniMap.create_key( 'slist', Java::java.util.List )
  SVAL  = UniMap.create_key( 'sval' )

  UniMap.define_accessors

  def test_remove_rejects
    ( -1 .. 2 ).each do |c|
      map = UniMap.new
      map.slist = ( [ UniMap.new ] * c if c >= 0 )
      f = sublist_filter( [ fltr { :reject } ] )
      f.filter( map )

      refute map.slist
    end
  end

  def test_do_not_remove_rejects_when_set
    [ -1, 1, 2 ].each do |c|
      map = UniMap.new
      map.slist = pre = ( [ UniMap.new ] * c if c >= 0 )
      f = sublist_filter( [ fltr { :reject } ], false )
      f.filter( map )

      assert_equal( pre, map.slist )
    end
  end

  def test_do_not_remove_accepted
    [ -1, 1, 2 ].each do |c|
      map = UniMap.new
      map.slist = pre = ( [ UniMap.new ] * c if c >= 0 )
      f = sublist_filter( [] ) #accept all
      f.filter( map )

      assert_equal( pre, map.slist )
    end
  end

  def test_modify
    ( -1 .. 2 ).each do |c|
      map = UniMap.new
      map.slist = pre = ( [ UniMap.new ] * c if c >= 0 )
      f = sublist_filter( [ fltr { |s| s.sval = :mod } ] )
      f.filter( map )

      assert_equal( Array( pre ).length,
                    Array( map.slist ).length )

      Array( map.slist ).each do |s|
        assert_equal( :mod, s.sval )
      end
    end
  end

  def sublist_filter( sfilters, remove_rejects = true )
     SubListFilter.new( SLIST,
                        FilterChain.new( "sub", sfilters ),
                        remove_rejects )
  end

end
