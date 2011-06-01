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

require 'iudex-http-test/server'

require 'minitest/unit'

module Iudex::HTTP::Test

  class CustomUnit < MiniTest::Unit

    def _run_suites(suites, type)
      super(suites, type)
    ensure
      Helper.stop
    end

  end

  module Helper

    def server
      Helper.server
    end

    def self.server
      @server ||= begin
                    server = Server.new
                    server.start
                    server
                  end
    end

    def self.stop
      $stderr.puts
      @server.stop if @server
      @server = nil
    end
  end

  MiniTest::Unit.runner = CustomUnit.new

end
