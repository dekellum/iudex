#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2014 David Kellum
#
# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License.  You may
# obtain a copy of the License at
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

require 'iudex-da'
require 'iudex-da/models'

class TestUrlModel < MiniTest::Unit::TestCase
  include Iudex::DA
  include Iudex::DA::ORM

  def setup
    Url.truncate
  end

  def test_round_trip
    urls = [ "http://foo.gravitext.com/bar/1",
             "http://gravitext.com/2",
             "http://hometown.com/33" ]

    urls.each do | u, p |
      Url.create( :visit_url => u, :type => "PAGE"  )
    end

    assert_equal( 2, Url.where( :domain => 'gravitext.com' ).count )
    assert_equal( 1, Url.where( :domain => 'hometown.com' ).count )

    sample = Url.find_by_url( urls[ 0 ] )
    assert_equal( urls[ 0 ], sample.url )
    assert_equal( 'PAGE', sample.type )

    refute( Url.find_by_url( "http://spunk" ) )
  end

end
