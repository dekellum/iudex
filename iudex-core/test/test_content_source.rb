#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2012 David Kellum
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

class TestContentSource < MiniTest::Unit::TestCase
  include Iudex::Core

  import 'java.nio.ByteBuffer'
  import 'java.nio.charset.Charset'

  def self.charset( name )
    Charset::for_name( name )
  end

  UTF8 = charset( "UTF-8" )
  ISO1 = charset( "ISO-8859-1" )

  def setup
    @cs = ContentSource.new( ByteBuffer::wrap( "any".to_java_bytes ) )
  end

  def test_default_encoding
    refute( @cs.default_encoding )
  end

  def test_default_encoding
    assert( @cs.set_default_encoding( UTF8, 0.0 ) )
    assert_equal( UTF8, @cs.default_encoding )
    assert_in_epsilon( 0.0, @cs.encoding_confidence )
  end

  def test_default_encoding_additive
    2.times { assert( @cs.set_default_encoding( UTF8, 0.10 ) ) }
    assert_equal( UTF8, @cs.default_encoding )
    assert_in_epsilon( 0.20, @cs.encoding_confidence )
  end

  def test_default_encoding_map
    assert( @cs.set_default_encoding( { UTF8 => f( 0.10 ),
                                        ISO1 => f( 0.20 ) } ) )
    assert_equal( ISO1, @cs.default_encoding )
    assert_in_epsilon( 0.20, @cs.encoding_confidence )

    refute( @cs.set_default_encoding( {} ) )
    refute( @cs.set_default_encoding( { UTF8 => f( 0.05 ) } ) )
    assert( @cs.set_default_encoding( { UTF8 => f( 0.07 ),
                                        ISO1 => f( 0.01 ) } ) )

    assert_equal( UTF8, @cs.default_encoding )
    assert_in_epsilon( 0.22, @cs.encoding_confidence )
  end

  def f( v )
    Java::java.lang.Float.new( v )
  end

end
