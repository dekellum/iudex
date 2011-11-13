#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2011 David Kellum
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
require 'iudex-da/ar'

class TestMigrate < MiniTest::Unit::TestCase
  include Iudex::DA
  include RJack

  VERBOSE = ! ( ARGV & %w[ -v --verbose ] ).empty?

  def setup
    unless VERBOSE
      Logback[ 'iudex.da.ActiveRecord' ].level = Logback::WARN
    end
  end

  def teardown
    Hooker.send( :clear )
    suppress_messages? { migrate }
    Logback[ 'iudex.da.ActiveRecord' ].level = nil
  end

  def test_default
    check_up_down
  end

  def test_simhash_profile
    Hooker.add( [ :iudex, :migration_profiles ] ) { |p| p << :simhash }
    check_up_down
  end

  def test_next_visit_profile
    Hooker.add( [ :iudex, :migration_profiles ] ) { |p| p << :index_next_visit }
    check_up_down
  end

  def check_up_down
    suppress_messages? do
      migrate
      pass
      migrate( 0 )
      pass
    end
  end

  def suppress_messages?( &block )
    if VERBOSE
      block.call
    else
      ActiveRecord::Migration.suppress_messages( &block )
    end
  end

end
