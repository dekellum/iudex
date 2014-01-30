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

require 'gravitext-util'
require 'iudex-filter/proc_filter'

class TestProcFilter < MiniTest::Unit::TestCase
  include Iudex::Filter
  include Gravitext::HTMap

  UniMap.create_key( 'mkey' )
  UniMap.define_accessors

  def test_describe_proc
    assert_equal( [ 'test_proc_filter', __LINE__ ], fltr {}.describe )
    assert_equal( [ 'test_proc_filter', __LINE__ ], ProcFilter.new {}.describe )
    assert_equal( [ 'test_proc_filter', __LINE__ ], ProcFilter.new {}.describe )
  end

  def test_name_proc
    index = Core::FilterIndex.new
    name, line = index.register( fltr {} ), __LINE__
    assert_equal( "i.f.ProcFilter-test_proc_filter-#{line}", name )
  end

  def test_describe_method
    assert_equal( [ :filter_m_reject ],
                  fltr_method( :filter_m_reject ).describe )
  end

  def test_name_method
    index = Core::FilterIndex.new
    name = index.register( fltr_method( :filter_m_reject ) )
    assert_equal( "i.f.ProcFilter-filter_m_reject", name )
  end

  def test_return_method
    assert do_f( fltr_method :filter_m_happy )
    refute do_f( fltr_method :filter_m_reject )
  end

  def filter_m_reject( map )
    :reject
  end

  def filter_m_happy( map )
  end

  def test_return
    assert do_f( fltr { } )
    assert do_f( fltr { nil } )
    assert do_f( fltr { true } )
    assert do_f( fltr { :other_sym } )
    assert do_f( fltr { false } ) # Consequence

    refute do_f( fltr { :reject } )
  end

  def test_mutate
    map = UniMap.new
    map.mkey = :initial
    assert do_f( fltr { |m| m.mkey = :mutated }, map )
    assert_equal( :mutated, map.mkey )
  end

  def do_f( f, m = UniMap.new )
    f.filter( m )
  end

end
