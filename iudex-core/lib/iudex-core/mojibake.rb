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

require 'iudex-core'
require 'java'

module Iudex::Core

  module MojiBake
    DEFAULT_CONFIG = File.join( File.dirname( __FILE__ ),
                                '..', '..', 'config', 'mojibake' )

    def self.load_config( file = DEFAULT_CONFIG )
      regex = nil
      mojis = []
      File.open( file ) do |fin|
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
      [ regex, mh ]
    end

    private

    def self.jstring( cps )
      cs = Array( cps ).map { |cp| cp.hex }.to_java( :char )
      Java::java.lang.String.new( cs )
    end

  end

  module Filters
    import 'iudex.core.filters.MojiBakeFilter'

    # Re-open iudex.core.filters.MojiBakeFilter to add config file
    # based initialization.
    class MojiBakeFilter

      # Alt constructor taking a configuration file in `mojibake -t`
      # format.
      def initialize( key, config_file = MojiBake::DEFAULT_CONFIG )
        super( key, *MojiBake.load_config( config_file ) )
      end

    end

  end

end
