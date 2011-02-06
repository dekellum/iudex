#--
# Copyright (c) 2011 David Kellum
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

require 'iudex-brutefuzzy-service/base.rb'

require 'rjack-qpid-client'
require 'iudex-core'
require 'iudex-simhash'
require 'iudex-brutefuzzy-protobuf'

require 'java'

module Iudex
  module BruteFuzzy
    module Service
      require "iudex-brutefuzzy-service/iudex-brutefuzzy-service-#{VERSION}.jar"

      import 'iudex.brutefuzzy.service.Service'
    end
  end

  module Core
    module Config
      @jms_context_proc = nil
      @fuzzy_tree_proc = nil

      def self.setup_jms_context( &block )
        @jms_context_proc = block
      end

      def self.do_jms_context( jms_context )
        @jms_context_proc.call( jms_context ) if @jms_context_proc
      end

      def self.setup_fuzzy_tree( &block )
        @fuzzy_tree_proc = block
      end

      def self.do_fuzzy_tree( fuzzy_tree )
        @fuzzy_tree_proc.call( fuzzy_tree ) if @fuzzy_tree_proc
      end

    end
  end
end

require 'iudex-brutefuzzy-service/agent'
