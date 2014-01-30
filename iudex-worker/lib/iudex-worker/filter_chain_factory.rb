#--
# Copyright (c) 2008-2014 David Kellum
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

require 'iudex-char-detector'

require 'iudex-html'
require 'iudex-html/factory_helper'

require 'iudex-simhash'
require 'iudex-simhash/factory_helper'

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
      include Iudex::CharDetector

      include Iudex::DA::Filters::FactoryHelper
      include Iudex::HTML::Filters::FactoryHelper
      include Iudex::SimHash::Filters::FactoryHelper
      include FetchHelper

      attr_accessor :http_client
      attr_accessor :data_source
      attr_accessor :visit_counter
      attr_accessor :executor
      attr_accessor :work_poller

      def initialize( name )
        super
        setup_reporters
      end

      def setup_reporters
        # Use default, preserved for overrides
      end

      def filters
        [ UHashMDCSetter.new,
          DefaultFilter.new,
          super,
          type_switch ]
      end

      def listeners
        super + [ MDCUnsetter.new( "uhash" ) ]
      end

      def type_map
        { "FEED" => [ feed_fetcher, :main ],
          "PAGE" => [ page_fetcher, :main ] }
      end

      def type_switch( tmap = type_map )
        create_switch( :type.to_k, tmap )
      end

      def feed_fetcher
        [ create_content_fetcher( :types => :feed_mime_types,
                                  :filters => :feed_receiver ) ]
      end

      def page_fetcher
        [ create_content_fetcher( :types => :page_mime_types,
                                  :filters => :page_receiver ) ]
      end

      def feed_receiver
        [ RedirectHandler.new,
          Revisitor.new( visit_counter ),
          RomeFeedParser.new,
          DefaultFilter.new,
          DateChangeFilter.new( false ),
          feed_updater ]
      end

      def feed_updater
        create_update_filter( :fields        => feed_update_keys,
                              :on_content    => :feed_post,
                              :on_referer    => :feed_post,
                              :on_ref_update => :feed_ref_update,
                              :on_ref_new    => :feed_ref_new )
      end

      def feed_ref_new
        [ UHashMDCSetter.new,
          ref_common_cleanup,
          Prioritizer.new( "feed-ref-new",
                           :constant => 50,
                           :min_next => 0.0 ) ]
      end

      def feed_ref_update
        [ UHashMDCSetter.new,
          DateChangeFilter.new( true ),
          ref_common_cleanup,
          Prioritizer.new( "feed-ref-update",
                           :constant => 10,
                           :min_next => 0.0 ) ]
      end

      def feed_post
        [ UHashMDCSetter.new,
          ref_common_cleanup,
          Prioritizer.new( "feed-post",
                           :constant => 30,
                           :visiting_now => true ),
          last_visit_setter ]
      end

      def ref_common_cleanup
        [ ref_html_filters,
          TextCtrlWSFilter.new( :title.to_k ),
          FutureDateFilter.new( :pub_date.to_k ) ]
      end

      def ref_html_filters
        [ html_clean_filters( :title ),
          html_clean_filters( :summary ),
          html_clean_filters( :content ),
          html_write_filter( :summary ),
          html_write_filter( :content ) ]
      end

      def feed_update_keys
        page_update_keys + [ :title, :summary, :content ]
      end

      def page_receiver
        [ RedirectHandler.new,
          Revisitor.new( visit_counter ),
          CharDetectFilter.new,
          html_clean_filters( :source ),
          simhash_generator,
          page_updater ]
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
        create_update_filter( :fields     => page_update_keys,
                              :on_content => :page_post,
                              :on_referer => :page_post )
      end

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
        [ :uhash, :domain, :url, :type,
          ( :reserved if work_poller && work_poller.reserve? ),
          ( :instance if work_poller && work_poller.instance ),
          :ref_pub_date, :pub_date,
          :priority, :last_visit, :next_visit_after,
          :status, :etag, :reason, :referer, :referent,
          :cache_file, :cache_file_offset, :simhash ].compact
      end

      def last_visit_setter
        resv = work_poller && work_poller.reserve?
        inst = work_poller && work_poller.instance
        [ Copier.new( *keys( :visit_start, :last_visit ) ),
          ( Setter.new( :reserved.to_k, nil )  if resv ),
          ( Setter.new( :instance.to_k, inst ) if inst ) ]
      end

    end

  end
end
