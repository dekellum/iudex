#--
# Copyright (c) 2008-2011 David Kellum
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
require 'hooker'

module Iudex

  module HTTPClient3
    require "#{LIB_DIR}/iudex-httpclient-3-#{VERSION}.jar"

    import 'iudex.httpclient3.HTTPClient3'

    def self.create_manager
      mgr = RJack::HTTPClient3::ManagerFacade.new

      # Sensible defaults:
      mgr.manager_params.max_total_connections = 100
      mgr.manager_params.default_max_connections_per_host = 2
      mgr.manager_params.stale_checking_enabled = false
      mgr.client_params.connection_manager_timeout = 3_000 #ms
      mgr.client_params.so_timeout = 5_000 #ms

      mgr.client_params.set_parameter(
          RJack::HTTPClient3::HttpMethodParams::RETRY_HANDLER,
          RJack::HTTPClient3::DefaultHttpMethodRetryHandler.new( 2, false ) )

      # FIXME: Use scoped per-session cookies?
      cp = Java::org.apache.commons.httpclient.cookie.CookiePolicy
      mgr.client_params.cookie_policy = cp::IGNORE_COOKIES

      Hooker.apply( [ :iudex, :http_client_3 ], mgr )

      mgr
    end

  end

end
