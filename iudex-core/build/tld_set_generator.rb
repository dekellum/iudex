#!/usr/bin/env jruby
# -*- ruby -*-

#--
# Copyright (c) 2008-2012 David Kellum
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

require 'erb'

# Generator for TLDSets.java from input effective_tld_name.dat
# See http://publicsuffix.org/
class TLDSetGenerator

  attr_reader :tlds, :tld_parents, :reg_exceptions

  BASEDIR = File.dirname( __FILE__ )

  INPUT_DAT = File.join( BASEDIR, 'effective_tld_name.dat' )

  JAVA_OUT  = File.join( BASEDIR, '..', 'src',
                         'main', 'java', 'iudex', 'core', 'TLDSets.java' )

  def run( tld_file = INPUT_DAT, java_file = JAVA_OUT )
    parse( tld_file )
    generate_java( java_file )
  end

  def parse( tld_file )
    @tlds           = []
    @tld_parents    = []
    @reg_exceptions = []

    open( tld_file, "r" ) do |fin|
      fin.each do |line|
        case line
        when %r{^\s*//}, /^\s*$/
          # ignore comment, empty lines
        when /^\s*([^\s\*\!]+)\s*$/
          @tlds << $1
        when /^\s*\*\.([^\s\*\!]+)\s*$/
          @tld_parents << $1
        when /^\s*\!([^\s\*\!]+)\s*$/
          @reg_exceptions << $1
        else
          raise "Parse ERROR: line [#{line}]"
        end
      end
    end

    [ @tlds, @tld_parents, @reg_exceptions ]
  end

  def generate_java( java_file )
    erb_file = File.join( BASEDIR, 'TLDSets.java.erb' )
    template = ERB.new( IO.read( erb_file ), nil, '%' )

    open( java_file, 'w' ) do |fout|
      fout << template.result( binding )
    end
  end

  def format_list( list )
    all = list.map { |d| '"' + d + '"' }.join( ", " )
    out = ""
    until( all.empty? )
      out << ' ' * 8
      if all.length > 71
        i = all.rindex( ',', 71 )
        out << all.slice!( 0..i )
        all.lstrip!
      else
        out << all
        all = ""
      end
      out << "\n" unless all.empty?
    end
    out
  end

end

if $0 == __FILE__
  TLDSetGenerator.new.run( *ARGV )
end
