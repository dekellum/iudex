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

require 'iudex-http-test/base'
require 'iudex-http-test/test_app'

require 'mizuno'

module Iudex::HTTP::Test

  class Server
    attr_accessor :port

    def initialize
      @handler = Mizuno::HttpServer
      # FIXME: Do the jetty setup ourselves?

      # FIXME: Have to use a fixed port, even for testing as can't get
      # arbitrary port back out of Mizuno.
      @port = 19292
    end

    def start
      @handler.run( TestApp.new,
                    :host => '0.0.0.0',
                    :port => @port,
                    :embedded => true )
    end

    def stop
      @handler.stop
    end

  end

end
