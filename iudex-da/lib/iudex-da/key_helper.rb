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

require 'iudex-da'
require 'iudex-filter/key_helper'

module Iudex
  module Filter
    module KeyHelper

      # Override to lookup matching Key in ContentMapper::LOGICAL_KEYS
      # or normal UniMap::KEY_SPACE
      def self.lookup_key( name )
        Iudex::DA::ContentMapper::LOGICAL_KEYS.get( name ) or
          lookup_key_space( name )
      end

    end
  end
end
