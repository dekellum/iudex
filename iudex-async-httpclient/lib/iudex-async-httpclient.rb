#--
# Copyright (c) 2011 David Kellum
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

require 'iudex-http'
require 'rjack-async-httpclient'
require 'hooker'

require 'iudex-async-httpclient/base'

require 'java'

module Iudex

  module AsyncHTTPClient
    require "#{LIB_DIR}/iudex-async-httpclient-#{VERSION}.jar"

    import 'iudex.asynchttpclient.Client'

    def self.create_client_config( opts = {} )

      cfg = { :connection_timeout_in_ms => 3_000,
              :idle_connection_timeout_in_ms => 6_000,
              :maximum_connections_total => 100,
              :maximum_connections_per_host => 2,
              :request_timeout_in_ms => 5_000,
              :max_request_retry => 2,
              :follow_redirects => true,
              :maximum_number_of_redirects => 6 }

      cfg = cfg.merge( opts )

      cfg = Hooker.merge( [ :iudex, :async_httpclient ], cfg )

      RJack::AsyncHTTPClient::build_client_config( cfg )
    end

  end

end
