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

require 'iudex-filter'
require 'iudex-filter/filter_chain_factory'

require 'iudex-core'

require 'iudex-da'
require 'iudex-da/factory_helper'

require 'iudex-rome'

module Iudex
  module Worker

    class FilterChainFactory < Iudex::Filter::Core::FilterChainFactory
      include Iudex::Filter::Core
      include Iudex::Core
      include Iudex::Core::Filters
      include Iudex::DA::Filters::FactoryHelper
      include Iudex::ROME

      attr_accessor :http_client
      attr_accessor :data_source

      def initialize( name )
        super
        setup_reporters
      end

      def setup_reporters
        add_summary_reporter
        add_by_filter_reporter
      end

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
        [ RomeFeedParser.new,
          DateChangeFilter.new( false ),
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
