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

require 'iudex-filter'

class Symbol
  def to_k
    Iudex::Filter::KeyHelper.lookup_key( self.to_s )
  end
end

class Gravitext::HTMap::Key
  def to_k
    self
  end
end

module Iudex
  module Filter

    # Mixin module support for UniMap Keys
    module KeyHelper

      # Lookup matching Key in UniMap::KEY_SPACE
      def self.lookup_key( name )
        lookup_key_space( name )
      end

      # Lookup matching Key in UniMap::KEY_SPACE
      def self.lookup_key_space( name )
        Gravitext::HTMap::UniMap::KEY_SPACE.get( name ) or
          raise( "Key #{name} not found" )
      end

      # Map Symbols to Keys
      def keys( *syms )
        syms.flatten.compact.map { |s| s.to_k }.uniq
      end

      module_function :keys
    end
  end
end
