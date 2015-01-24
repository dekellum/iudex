#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2015 David Kellum
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

class TestLogWriter < MiniTest::Unit::TestCase
  import 'java.io.PrintWriter'
  import 'iudex.util.LogWriter'
  import 'java.util.regex.Pattern'

  def test_log_writer
    lw = LogWriter.new( 'TestLogWriter' )
    lw.remove_pattern = Pattern.compile( '(^test)|(\s+$)' )
    pw = PrintWriter.new( lw, true )
    pw.print( "test 1   \n\r" )
    pw.println( "test 2   \n\r" )
    pw.print( "test 3" )

    #FIXME: Assert?
  end

end
