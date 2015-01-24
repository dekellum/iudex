#--
# Copyright (c) 2011-2015 David Kellum
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

module Iudex::BruteFuzzy::Service

  module Destinations

    def self.apply( ctx )

      ctx.destinations[ 'brutefuzzy-response-ex' ] = {
        :assert => :sender,
        :create => :sender,
        :node   => {
          :type       => :topic,
          'x-declare' => {
            :type     => :fanout,
          }
        }
      }

      # Direct request writes are are needed for querying depth
      # (explicit flow control). Thus no exchange here.
      ctx.destinations[ 'brutefuzzy-request' ] = {
        :assert => :always,
        :create => :always,
        :node   => {
          :type       => :queue,
          'x-declare' => {
            :arguments => {
              'qpid.max_size'    => 500_000,
              'qpid.policy_type' => :reject,
            }
          }
        }
      }

      ctx.destinations[ 'brutefuzzy-client' ] = {
        :address => ctx.address_per_process( 'brutefuzzy-client' ),
        :assert  => :receiver,
        :create  => :receiver,
        :delete  => :receiver,
        :node    => {
          :type        => :queue,
          'x-bindings' => [ { :exchange => 'brutefuzzy-response-ex' } ],
          'x-declare'  => {
            'auto-delete' => true,
            :arguments    => {
              'qpid.max_size'    => 500_000,
              'qpid.policy_type' => :ring,
            }
          }
        }
      }

    end
  end

end
