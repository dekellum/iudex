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

require 'iudex-core'

require 'iudex-worker/base'

module Iudex
  module Worker
  end

  module Core
    module Config
      @agent_proc = nil
      @filter_factory_proc = nil
      @work_poller_proc = nil

      def self.setup_agent( &block )
        @agent_proc = block
      end

      def self.do_agent( agent )
        @agent_proc.call( agent ) if @agent_proc
      end

      def self.setup_filter_factory( &block )
        @filter_factory_proc = block
      end

      def self.do_filter_factory( filter_factory )
        @filter_factory_proc.call( filter_factory ) if @filter_factory_proc
      end

      def self.setup_work_poller( &block )
        @work_poller_proc = block
      end

      def self.do_work_poller( work_poller )
        @work_poller_proc.call( work_poller ) if @work_poller_proc
      end

    end
  end

end

require 'iudex-worker/agent'
