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

require 'iudex-da'
require 'iudex-da/pool_data_source_factory'

require 'iudex-httpclient-3' #FIXME: More central?
require 'iudex-rome'

module Iudex
  module Worker

    class FilterChainFactory < Iudex::Core::FilterChainFactory
      import 'iudex.core.filters.UHashMDCSetter'
      import 'iudex.core.filters.ContentFetcher'
      import 'iudex.core.filters.TextCtrlWSFilter'

      import 'iudex.da.feed.FutureDateFilter'
      import 'iudex.da.feed.FeedWriter'

      import 'iudex.httpclient3.HTTPClient3' #FIXME
      import 'iudex.rome.RomeFeedParser' #FIXME

      def feed_post
        [ RomeFeedParser.new, #FIXME: dep leak.
          feed_writer ]
      end

      def feed_writer
        f = FeedWriter.new( data_source, [] )

        create_chain( "feed-update", feed_update ) { |c| f.update_ref_filter = c }
        create_chain( "feed-new",    feed_new )    { |c| f.new_ref_filter = c }
        f
      end

      # FIXME: Central configuration?
      def http_client
        @http_mf = RJack::HTTPClient3::ManagerFacade.new
        @http_mf.start
        HTTPClient3.new( @http_mf.client )
        #FIXME: Close with FilterChainFactory?
      end

      def feed_filters
        [ ContentFetcher.new( http_client,
                              create_chain( "feed-receiver", feed_post ) ) ]
      end

      def feed_update
        [ TextCtrlWSFilter.new( ContentKeys::TITLE ),
          FutureDateFilter.new( ContentKeys::PUB_DATE ) ]
      end

      def feed_new
        [ TextCtrlWSFilter.new( ContentKeys::TITLE ),
          FutureDateFilter.new( ContentKeys::PUB_DATE ) ]
      end

      # FIXME: From DA?
      def data_source
        factory = Iudex::DA::PoolDataSourceFactory.new
        factory.create
      end

    end
  end
end
