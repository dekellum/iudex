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
require 'iudex-da/filter_chain_factory'
require 'iudex-da/pool_data_source_factory'

require 'iudex-httpclient-3' #FIXME: More central?
require 'iudex-rome'

module Iudex
  module Worker

    class FilterChainFactory < Iudex::DA::FilterChainFactory

      import 'iudex.httpclient3.HTTPClient3'
      import 'iudex.rome.RomeFeedParser'

      # FIXME: More Central configuration?
      def initialize( name )
        super( name )
        @http_mf = RJack::HTTPClient3::ManagerFacade.new
        @http_mf.start
        self.http_client = HTTPClient3.new( @http_mf.client )

        #FIXME: Close with FilterChainFactory?

        self.data_source = Iudex::DA::PoolDataSourceFactory.new.create
      end

      def feed_post
        [ RomeFeedParser.new ] + super
      end

    end
  end
end
