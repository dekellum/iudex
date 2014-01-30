#--
# Copyright (c) 2008-2014 David Kellum
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

      # Create a ContentFetcher including a filter chain to receive
      # the fetch result.
      #
      # === Options
      #
      # Options support literal values, or a Proc, Method, or a Symbol
      # to self send unless otherwise noted.
      #
      # :types:: An Array or table of Mime types use Accept header in
      #          default :request_headers and to restrict returned
      #          results on. (Default: #page_mime_types)
      #
      # :client:: The Java::iudex.http.HTTPClient implementation to
      #           use (Default: :http_client)
      #
      # :user_agent:: The HTTP User-Agent for default
      #               :request_headers. Proc's will receive the
      #               options Hash as parameter (Default: #http_user_agent)
      #
      # :visit_counter:: The Java::iudex.core.VisitCounter
      #                  implementation. (Default: :visit_counter)
      #
      # :executor:: The java.util.concurrent.Executor to use for
      #             running the receiver filter chain. (Default: :executor)
      #
      # :request_headers:: HTTP Request headers as Array<iudex.http.Header>
      #                    (Default: #http_request_headers)
      #
      # All options (including the required :filters option, and
      # :listener with default :main) are also passed to
      # self.create_chain for creating the receiver filter chain
      #
      # The positional parameters equivalent to ( :types, :filters,
      # :listener ) as defined above are also supported, but
      # deprecated.
      #
      def create_content_fetcher( *args )
        opts = args.last.is_a?( Hash ) ? args.pop.dup : {}

        opts[ :types ]    ||= args.shift
        opts[ :filters  ] ||= args.shift
        opts[ :listener ] ||= args.shift

        opts = { :types           => :page_mime_types,
                 :listener        => :main,
                 :client          => :http_client,
                 :user_agent      => :http_user_agent,
                 :visit_counter   => :visit_counter,
                 :executor        => :executor,
                 :request_headers => :http_request_headers
               }.merge( opts )

        cf = ContentFetcher.new( call_if( opts[ :client ] ),
                                 call_if( opts[ :visit_counter ] ),
                                 create_chain( opts ) )

        cf.executor = call_if( opts[ :executor ] )

        alist = accept_list( call_if( opts[ :types ] ) )
        unless alist.include?( '*/*' )
          cf.accepted_content_types = ContentTypeSet.new( alist )
        end

        cf.request_headers = call_if( opts[ :request_headers ], opts )
        cf
      end

      def http_request_headers( opts )
        [ [ 'User-Agent', call_if( opts[ :user_agent ] ) ],
          [ 'Accept',     accept_header( call_if( opts[ :types ] ) ) ]
        ].map { |kv| Header.new( *kv ) }
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

      def call_if( v, *args )
        if v.is_a?( Proc ) || v.is_a?( Method )
          v.call( *args )
        elsif v.is_a?( Symbol )
          send( v, *args )
        else
          v
        end
      end

    end

  end
end
