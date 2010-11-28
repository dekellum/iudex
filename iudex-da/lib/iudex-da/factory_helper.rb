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
require 'iudex-da/key_helper'
require 'iudex-da/pool_data_source_factory'

module Iudex
  module DA
    module Filters

      # Mixin FilterChainFactory helper methods
      module FactoryHelper
        include Iudex::Filter::KeyHelper

        # Lazy initialize DataSource
        def data_source
          @data_source ||= PoolDataSourceFactory.new.create
        end

        # Create UpdateFilter given fields and filter list factory
        # methods
        def create_update_filter( fields = [],
                                  post_sym = nil,
                                  update_sym = nil,
                                  new_sym = nil )
          fields = ( keys( :uhash ) + fields ).uniq

          f = UpdateFilter.new( data_source, ContentMapper.new( fields ) )
          create_chain( update_sym ) { |c| f.update_ref_filter = c }
          create_chain( new_sym )    { |c| f.new_ref_filter    = c }
          create_chain( post_sym )   { |c| f.content_filter    = c }
          f
        end

      end

    end
  end
end
