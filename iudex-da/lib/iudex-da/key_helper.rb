#--
# Copyright (c) 2008-2010 David Kellum
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

require 'iudex-da'

class Symbol

  # Lookup matching Key in ContentMapper::LOGICAL_KEYS or normal
  # UniMap::KEY_SPACE
  def to_k
    n = self.to_s
    ( Iudex::DA::ContentMapper::LOGICAL_KEYS.get( n ) or
      Gravitext::HTMap::UniMap::KEY_SPACE.get( n ) or
      raise( "Key #{n} not found" ) )
  end
end

module Iudex
  module DA

    # Mixin module support for UniMap Keys
    module KeyHelper

      # Map Symbols to Keys
      def keys( *syms )
        syms = syms[0] if ( syms[0] && syms[0].respond_to?( :each ) )
        syms.map { |s| s.to_k }.uniq
      end
      module_function :keys
    end
  end
end
