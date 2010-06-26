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
require 'iudex-core'
require 'iudex-filter'
require 'iudex-filter/filter_chain_factory'

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

      # FIXME: To move into iudex-worker
      class FilterChainFactory < Iudex::Filter::Core::FilterChainFactory
        include Iudex::DA::Filters::FactoryHelper
        include Iudex::Filter::Core
        include Iudex::Core
        include Iudex::Core::Filters

        attr_accessor :http_client

        def filters
          [ UHashMDCSetter.new ] + super + [ type_switch ]
        end

        def listeners
          super + [ MDCUnsetter.new( "uhash" ) ]
        end

        def type_map
          { "FEED" => feed_fetcher,
            "PAGE" => page_fetcher }
        end

        def type_switch( tmap = type_map )
          create_switch( ContentKeys::TYPE, tmap )
        end

        def feed_fetcher
          [ ContentFetcher.new( http_client,
                                create_chain( "feed-receiver", feed_receiver ) )
          ]
        end

        def page_fetcher
        end

        def feed_receiver
          [ DateChangeFilter.new( false ),
            feed_updater ]
        end

        def feed_updater
          create_update_filter( :feed_ref_update, :feed_ref_new, :feed_post,
                                more_feed_update_fields )
        end

        def feed_ref_update
          [ UHashMDCSetter.new,
            DateChangeFilter.new( true ),
            Prioritizer.new( "feed-ref-update" ) ] +
            ref_common_cleanup
        end

        def feed_ref_new
          [ UHashMDCSetter.new,
            Prioritizer.new( "feed-ref-new" ) ] +
            ref_common_cleanup
        end

        def feed_post
          [ UHashMDCSetter.new,
            Prioritizer.new( "feed-post" ) ] +
            ref_common_cleanup
        end

        def ref_common_cleanup
          [ TextCtrlWSFilter.new( ContentKeys::TITLE ),
            FutureDateFilter.new( ContentKeys::PUB_DATE ) ]
        end

        def more_feed_update_fields
          []
        end

      end
    end
  end
end
