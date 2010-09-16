#!/usr/bin/env jruby
# -*- ruby -*-

#--
# Copyright (c) 2010 David Kellum
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
require 'ostruct'

# Generator for HTML.java tags/attribute input configuration
class JavaGenerator

  attr_reader :tags, :attributes

  BASEDIR = File.dirname( __FILE__ )

  JAVA_OUT  = File.join( BASEDIR, '..', 'src',
                         'main', 'java', 'iudex', 'html', 'HTML.java' )

  def run( java_file = JAVA_OUT )
    parse_tags
    parse_attributes
    generate_java( java_file )
  end

  def parse_tags()
    @tags = []

    open( File.join( BASEDIR, 'tags' ), 'r' ) do |fin|
      fin.each do |line|
        case line
        when /^\s*#/, /^\s*$/
          # ignore comment, empty lines
        when /^\s*[^\s,]+\s*,[^,]*,[^,]*$/
          r = line.split(',').map { |c| c.strip }
          r = r.compact.reject { |c| c.empty? }
          # FIXME: Handler flags?
          @tags << OpenStruct.new( :name => r[0], :desc => r[3] )
        else
          raise "Parse ERROR: line [#{line}]"
        end
      end
    end

    @tag_max_len = @tags.map { |t| t.name.length }.max
    [ @tags ]
  end

  def parse_attributes()
    @attributes = []

    open( File.join( BASEDIR, 'attributes' ), 'r' ) do |fin|
      fin.each do |line|
        case line
        when /^\s*#/, /^\s*$/
          # ignore comment, empty lines
        when /^\s*[^\s,]+\s*,/
          r = line.split(',').map { |c| c.strip }
          r = r.compact.reject { |c| c.empty? }
          # FIXME: Handle attributes, desc.
          @attributes << OpenStruct.new( :name => r[0], :desc => r[2] )
        else
          raise "Parse ERROR: line [#{line}]"
        end
      end
    end

    @attr_max_len = @attributes.map { |t| t.name.length }.max
    [ @attributes ]
  end

  def twidth( val, extra = 0 )
    val + ( ' ' * ( @tag_max_len - val.length + extra )  )
  end

  def awidth( val, extra = 0 )
    val + ( ' ' * ( @attr_max_len - val.length + extra )  )
  end

  def const( val )
    val.gsub( /\-/, '_' )
  end

  def generate_java( java_file )
    erb_file = File.join( BASEDIR, 'HTML.java.erb' )
    template = ERB.new( IO.read( erb_file ), nil, '%' )

    open( java_file, 'w' ) do |fout|
      fout << template.result( binding )
    end
  end

end

if $0 == __FILE__
  JavaGenerator.new.run( *ARGV )
end
