#--
# Copyright (c) 2008-2013 David Kellum
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

        # Create an UpdateFilter given the provided options.
        #
        # === Options
        #
        # :fields:: The Array of fields (Symbol or Key) to (re-)read
        #           from the database and update. The required :uhash
        #           field is included automatically.
        #
        # :max_retries:: Maximum number of retries not including the
        #                initial attempt, in case of a database
        #                conflict (Default: 3)
        #
        # :isolation_level:: A transaction isolation constant as
        #                    defined in java.sql.Connection
        #                    (Default: REPEATABLE_READ 0x04)
        #
        # :on_content:: Filter option for current (content) UniMap.
        #
        # :on_ref_update:: Filter option for REFERENCES that are
        #                  already existing in the database
        #
        # :on_ref_new:: Filter option for REFERENCES that are new (not
        #               found in db).
        #
        # :on_referer:: Filter option for the REFERER to the current
        #               content.
        #
        # The positional parameters equivalent to
        # ( :fields, :on_content, :on_ref_update, :on_ref_new )
        # as defined above are also supported but deprecated.
        #
        # === Filter options
        #
        # Each of the on_* filter options defined above may take a
        # value of a Filter, Array of filters or a Symbol value. The
        # following symbol values are special, all other Symbol values
        # are passed as the :filters option to create_chain, and is
        # interpreted as a method name.
        #
        # :merge:: The default behavior of merging the in-memory state
        #          with the current database state. This is equivalent
        #          to providing a NoOpFilter.
        #
        # :ignore:: Do not update this element. This is equivalent to
        #           providing a filter that always rejects, but faster
        #           since it need not read the value from the db.
        #
        def create_update_filter( *args )
          opts = args.last.is_a?( Hash ) ? args.pop.dup : {}

          opts[ :fields ]        ||= args.shift #deprecated
          opts[ :on_content ]    ||= args.shift
          opts[ :on_ref_update ] ||= args.shift
          opts[ :on_ref_new    ] ||= args.shift

          f = UpdateFilter.new( data_source, field_mapper( opts[ :fields ] ) )
          updater_chain( opts[:on_ref_update]) { |c| f.update_ref_filter = c }
          updater_chain( opts[ :on_ref_new ] ) { |c| f.new_ref_filter    = c }
          updater_chain( opts[ :on_content ] ) { |c| f.content_filter    = c }
          updater_chain( opts[ :on_referer ] ) { |c| f.referer_filter    = c }

          f.max_retries     = opts[ :max_retries ]   if opts[ :max_retries ]
          f.isolation_level = opts[:isolation_level] if opts[:isolation_level]

          f
        end

        # Create a ReadFilter given the provided options.
        #
        # === Options
        #
        # :fields:: The Array of fields (Symbol or Key) to read from
        #           the database.
        #
        # The positional parameters equivalent to ( :fields ) as
        # defined above is also supported but deprecated.
        #
        def create_read_filter( *args )
          opts = args.last.is_a?( Hash ) ? args.pop.dup : {}
          opts[ :fields ]        ||= args.shift #deprecated

          ReadFilter.new( data_source, field_mapper( opts[ :fields ] ) )
        end

        def field_mapper( fields )
          fields = keys( ( [ :uhash ] + fields ).flatten.compact )
          ContentMapper.new( fields )
        end

        def updater_chain( v, &block )
          if v.is_a?( Iudex::Filter::Filter )
            block.call( v )
          elsif v == :merge
            block.call( UpdateFilter::DEFAULT_MERGE )
          elsif v == :ignore
            block.call( nil )
          else
            create_chain( :filters => v, &block )
          end
        end

      end

    end
  end
end
