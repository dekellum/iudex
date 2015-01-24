#--
# Copyright (c) 2008-2015 David Kellum
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

require 'rjack-slf4j'
require 'optparse'
require 'hooker'

Hooker.log_with { |m| RJack::SLF4J[ 'iudex' ].info( m.rstrip ) }

module Iudex

  # Apply configuration from block
  def self.configure( &block )
    Hooker.with( :iudex, &block )
  end

  module Core

    # <b>DEPRECATED:</b> Extensible module defining top level
    # configuration blocks.
    module Config

      # <b>DEPRECATED:</b> Parse options using an OptionParser,
      # defining (-c)onfig option, and yielding to block for further
      # option handling.
      def self.parse_options( args = ARGV, &block )
        warn( "DEPRECATED parse_options called from #{caller.first.to_s}\n" +
              "Use Hooker.register_config and OptionParser.new instead." )
        parser = OptionParser.new do |opts|
          Hooker.register_config( opts )
          block.call( opts ) if block
        end
        parser.parse!
      end

    end
  end
end
