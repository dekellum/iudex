#--
# Copyright (c) 2008-2010 David Kellum
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

#### General test setup: LOAD_PATH, logging, console output ####

ldir = File.join( File.dirname( __FILE__ ), "..", "lib" )
$LOAD_PATH.unshift( ldir ) unless $LOAD_PATH.include?( ldir )

require 'rubygems'
require 'rjack-logback'
require 'minitest/unit'
require 'minitest/autorun'

module TestSetup
  include RJack
  Logback.config_console( :stderr => true )
  if ARGV.include?( '--verbose' ) || ARGV.include?( '-v' )
    Logback.root.level = Logback::DEBUG
  end
end

# Make test output logging compatible: no partial lines.
class TestOut
  def print( *a ); $stdout.puts( *a ); end
  def puts( *a );  $stdout.puts( *a ); end
end
MiniTest::Unit.output = TestOut.new
