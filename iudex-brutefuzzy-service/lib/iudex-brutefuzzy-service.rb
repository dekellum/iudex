#--
# Copyright (c) 2008-2012 David Kellum
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
require 'iudex-simhash'
require 'iudex-brutefuzzy-protobuf'

require 'rjack-qpid-client'

require 'iudex-brutefuzzy-service/base.rb'

require 'java'

module Iudex
  module BruteFuzzy
    module Service
      require "iudex-brutefuzzy-service/iudex-brutefuzzy-service-#{VERSION}.jar"

      import 'iudex.brutefuzzy.service.Service'
    end
  end
end

require 'iudex-brutefuzzy-service/agent'
