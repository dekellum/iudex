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

module Iudex
  module DA
    module Filters

      # Mixin FilterChainFactory helper methods
      module FactoryHelper

        # Lazy initialize DataSource
        def data_source
          @data_source ||= PoolDataSourceFactory.new.create
        end

        # Create UpdateFilter given filter list factory methods
        # symbols and any more_fields to read/writer.
        def create_update_filter( update_sym, new_sym, post_sym, more_fields )
          f = UpdateFilter.new( data_source, more_fields )

          create_chain( s_to_n( update_sym ), send( update_sym ) ) do |c|
            f.update_ref_filter = c
          end
          create_chain( s_to_n( new_sym ), send( new_sym ) ) do |c|
            f.new_ref_filter = c
          end
          create_chain( s_to_n( post_sym ), send( post_sym ) ) do |c|
            f.content_filter = c
          end

          f
        end

        private

        def s_to_n( sym )
          sym.to_s.gsub( /_/, '-' )
        end
      end

    end
  end
end
