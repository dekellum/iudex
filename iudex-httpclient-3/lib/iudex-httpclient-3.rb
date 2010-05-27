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

require 'iudex-http'
require 'rjack-httpclient-3'

require 'iudex-httpclient-3/base'

module Iudex
  module HttpClient3
    require "#{LIB_DIR}/iudex-httpclient-3-#{VERSION}.jar"
  end

  module Core
    module Config

      # Setup singleton ManagerFacade with defaults, yielding to block
      # for overrides.
      def self.setup_http_client_3

        @manager = RJack::HTTPClient3::ManagerFacade.new

        # Sensible defaults:
        @manager.manager_params.max_total_connections = 100
        @manager.manager_params.default_max_connections_per_host = 2
        @manager.manager_params.stale_checking_enabled = false
        @manager.client_params.connection_manager_timeout = 3_000 #ms
        @manager.client_params.so_timeout = 5_000 #ms

        @manager.client_params.set_parameter(
          RJack::HTTPClient3::HttpMethodParams::RETRY_HANDLER,
          RJack::HTTPClient3::DefaultHttpMethodRetryHandler.new( 2, false ) )

        # FIXME: Use scoped per-session cookies
        cp = Java::org.apache.commons.httpclient.cookie.CookiePolicy
        @manager.client_params.cookie_policy = cp::IGNORE_COOKIES

        yield @manager if block_given?
        @manager
      end

      # Return default/customized manager
      def self.http_client_3_manager
        @manager || setup_http_client_3
      end
    end
  end

end
