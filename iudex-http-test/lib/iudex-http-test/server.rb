#--
# Copyright (c) 2008-2013 David Kellum
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

require 'fishwife'

module Iudex::HTTP::Test

  class Server < Fishwife::HttpServer

    DEFAULT_PORT = 19292

    def initialize( opts = {} )
      opts = { :host => '127.0.0.1',
               :port => DEFAULT_PORT,
               :max_threads => 20 }.merge( opts )
      super( opts )
    end

    def start( app = TestApp )
      super
    end

  end

end
