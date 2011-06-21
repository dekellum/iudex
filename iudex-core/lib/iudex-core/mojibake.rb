#--
# Copyright (c) 2011 David Kellum
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

require 'iudex-core'
require 'java'

module Iudex::Core::Filters
  import 'iudex.core.filters.MojiBakeFilter'

  # Re-open iudex.core.filters.MojiBakeFilter to add config file
  # based initialization.
  class MojiBakeFilter

    DEFAULT_CONFIG = File.join( File.dirname( __FILE__ ),
                                '..', '..', 'config', 'mojibake' )

    # Alt constructor taking a configuration file in `mojibake -t`
    # format.
    def initialize( key, config_file = DEFAULT_CONFIG )
      regex = nil
      mojis = []
      File.open( config_file ) do |fin|
        fin.each do |line|
          case line
          when %r{^/([^/]+)/$}
            regex = $1
          when /^\[.*?\]\s+([0-9A-F ]+)\s+\[.*\]\s+([0-9A-F]+)$/
            mojis << [ $1.split( ' ' ), $2 ]
          end
        end
      end

      mh = Java::java.util.HashMap.new( 512 )
      mojis.each do | moji, rpl |
        mh.put( jstring( moji ), jstring( rpl ) )
      end
      super( key, regex, mh )
    end

    private

    def jstring( cps )
      cs = cps.map { |cp| cp.hex }.to_java( :char )
      Java::java.lang.String.new( cs )
    end

  end
end
