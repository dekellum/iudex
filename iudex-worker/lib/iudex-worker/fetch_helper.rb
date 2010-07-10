#--
# Copyright (c) 2008-2010 David Kellum
#
# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License.  You
# may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.  See the License for the specific language governing
# permissions and limitations under the License.
#++

require 'iudex-worker'

module Iudex
  module Worker

    module FetchHelper
      include Iudex::HTTP
      include Iudex::Core::Filters

      def create_content_fetcher( accept_types, receiver_sym )
        cf = ContentFetcher.new( http_client, create_chain( receiver_sym ) )

        alist = accept_list( accept_types )
        cf.accepted_content_types = alist unless alist.include?( '*/*' )

        headers = [ [ 'User-Agent', http_user_agent ],
                    [ 'Accept',     accept_header( accept_types ) ] ]

        puts headers.inspect

        cf.request_headers = headers.map { |kv| Header.new( *kv ) }

        cf
      end

      def http_user_agent
        ( "Mozilla/5.0 (compatible; " +
          "Iudex #{Iudex::Worker::VERSION}; " +
           "+http://gravitext.com/iudex)" )
      end

      def feed_mime_types
        # List of accepted mime types grouped and order in descending
        # order of preference.
        [ %w[ application/atom+xml application/rss+xml ],
          %w[ application/rdf+xml application/xml ],
          %w[ text/xml ],
          %w[ text/* ],
          %w[ */* ] ]
      end

      def page_mime_types
        [ %w[ application/xhtml+xml text/html ],
          %w[ application/xml ],
          %w[ text/* ] ]
      end

      def accept_header( types )
        q = 1.0
        ts = types.map do |tgrp|
          tgrp = tgrp.map { |m| "#{m};q=#{q}" } if q < 1.0
          q -= 0.1
          tgrp
        end
        ts.flatten.join( ',' )
      end

      def accept_list( types )
        types.flatten
      end

    end

  end
end
