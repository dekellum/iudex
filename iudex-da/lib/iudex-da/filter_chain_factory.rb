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

require 'iudex-core'
require 'iudex-core/filter_chain_factory'

module Iudex
  module DA

    class FilterChainFactory < Iudex::Core::FilterChainFactory
      import 'iudex.core.filters.UHashMDCSetter'
      import 'iudex.core.filters.ContentFetcher'
      import 'iudex.core.filters.TextCtrlWSFilter'
      import 'iudex.core.filters.Prioritizer'
      import 'iudex.core.filters.DateChangeFilter'
      import 'iudex.core.filters.FutureDateFilter'
      import 'iudex.da.filters.UpdateFilter'

      attr_accessor :data_source
      attr_accessor :http_client

      def feed_filters
        [ ContentFetcher.new( http_client,
                              create_chain( "feed-receiver", feed_post ) ) ]
      end

      def feed_post
        [ DateChangeFilter.new( false ),
          feed_writer ]
      end

      def feed_writer
        f = UpdateFilter.new( data_source, additional_writer_fields )

        create_chain( "ref-update", ref_update ) {|c| f.update_ref_filter = c}
        create_chain( "ref-new",    ref_new )    {|c| f.new_ref_filter    = c}
        create_chain( "content-post", content_post ) {|c| f.content_filter= c}
        f
      end

      def ref_update
        [ UHashMDCSetter.new,
          DateChangeFilter.new( true ),
          Prioritizer.new( "ref-update" ) ] +
          ref_common_cleanup
      end

      def ref_new
        [ UHashMDCSetter.new,
          Prioritizer.new( "ref-new" ) ] +
        ref_common_cleanup
      end

      def content_post
        [ UHashMDCSetter.new,
          Prioritizer.new( "content-post" ) ] +
        ref_common_cleanup
      end

      def ref_common_cleanup
        [ TextCtrlWSFilter.new( ContentKeys::TITLE ),
          FutureDateFilter.new( ContentKeys::PUB_DATE ) ]
      end

      def additional_writer_fields
        []
      end

    end
  end
end
