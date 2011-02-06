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

require 'iudex-filter'
require 'iudex-filter/filter_chain_factory'

require 'iudex-barc'

require 'iudex-core'

require 'iudex-da'
require 'iudex-da/factory_helper'

require 'iudex-rome'

require 'iudex-html'
require 'iudex-html/factory_helper'

require 'iudex-worker'
require 'iudex-worker/fetch_helper'
require 'iudex-worker/prioritizer'

module Iudex
  module Worker

    class FilterChainFactory < Iudex::Filter::Core::FilterChainFactory
      include Iudex::Filter::Core
      include Iudex::BARC
      include Iudex::Core
      include Iudex::Core::Filters
      include Iudex::ROME

      include Iudex::DA::Filters::FactoryHelper
      include Iudex::HTML::Filters::FactoryHelper
      include FetchHelper

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
        [ UHashMDCSetter.new,
          DefaultFilter.new,
          super,
          type_switch ].flatten
      end

      def listeners
        super + [ MDCUnsetter.new( "uhash" ) ]
      end

      def type_map
        { "FEED" => feed_fetcher,
          "PAGE" => page_fetcher }
      end

      def type_switch( tmap = type_map )
        create_switch( :type.to_k, tmap )
      end

      def feed_fetcher
        [ create_content_fetcher( feed_mime_types, :feed_receiver ) ]
      end

      def page_fetcher
        [ create_content_fetcher( page_mime_types, :page_receiver ) ]
      end

      def feed_receiver
        [ RomeFeedParser.new,
          DefaultFilter.new,
          DateChangeFilter.new( false ),
          feed_updater ]
      end

      def feed_updater
        create_update_filter( keys( feed_update_keys ),
                              :feed_post, :feed_ref_update, :feed_ref_new )
      end

      def feed_ref_new
        [ UHashMDCSetter.new,
          ref_common_cleanup,
          Prioritizer.new( "feed-ref-new",
                           :constant => 50,
                           :min_next => 0.0 ) ].flatten
      end

      def feed_ref_update
        [ UHashMDCSetter.new,
          DateChangeFilter.new( true ),
          ref_common_cleanup,
          Prioritizer.new( "feed-ref-update",
                           :constant => 10,
                           :min_next => 0.0 ) ].flatten
      end

      # Note: *_post is run possibly twice, once for both base content
      # map and referer map.
      def feed_post
        [ UHashMDCSetter.new,
          ref_common_cleanup,
          Prioritizer.new( "feed-post",
                           :constant => 30,
                           :visiting_now => true ),
          last_visit_setter ].flatten
      end

      def ref_common_cleanup
        [ ref_html_filters,
          TextCtrlWSFilter.new( :title.to_k ),
          FutureDateFilter.new( :pub_date.to_k ) ].flatten
      end

      def ref_html_filters
        [ html_clean_filters( :title ),
          html_clean_filters( :summary ),
          html_clean_filters( :content ),
          html_write_filter( :summary ),
          html_write_filter( :content ) ].flatten
      end

      def feed_update_keys
        page_update_keys + [ :title, :summary, :content ]
      end

      def page_receiver
        [ page_updater ]
      end

      def barc_writer
        bw = BARCWriter.new( barc_directory )
        bw.do_compress = true
        bw
      end

      def barc_directory
        bdir = BARCDirectory.new( Java::java.io.File.new( "./barc" ) )
        bdir
      end

      def page_updater
        create_update_filter( keys( page_update_keys ), :page_post )
      end

      # Note: *_post is run possibly twice, once for both base content
      # map and referer map.
      def page_post
        [ UHashMDCSetter.new,
          barc_writer, # Not run in 302 referer case, since no SOURCE.
          Prioritizer.new( "page-post",
                           :constant => 0,
                           :min_next => ( 30 * 60.0 ),
                           :visiting_now => true ),
          last_visit_setter ]
      end

      def page_update_keys
        [ :uhash, :host, :url, :type,
          :ref_pub_date, :pub_date,
          :priority, :last_visit, :next_visit_after,
          :status, :etag, :reason, :referer, :referent,
          :cache_file, :cache_file_offset ]
      end

      def last_visit_setter
        Copier.new( *keys( :visit_start, :last_visit ) )
      end

    end

  end
end
