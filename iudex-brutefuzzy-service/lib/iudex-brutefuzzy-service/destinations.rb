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

module Iudex::BruteFuzzy::Service

  module Destinations

    # http://apache-qpid-users.2158936.n2.nabble.com/JMS-Dyname-Ring-Queues-td5813023.html

    def self.apply( ctx )

      ctx.destinations[ 'iudex-brutefuzzy-request' ] = {
        :assert => :receiver,
        :create => :receiver,
        :node   => {
          :type       => :queue,
          'x-declare' => {
            :arguments => {
              'qpid.max_size'    => 200_000,
              'qpid.policy_type' => :reject,
            }
          }
        }
      }

      ctx.destinations[ 'iudex-brutefuzzy-response' ] = {
        :assert => :sender,
        :create => :sender,
        :node   => {
          :type       => :topic,
          'x-declare' => {
            :type     => :fanout,
            :exchange => 'iudex-brutefuzzy-response'
          }
        }
      }

      ctx.destinations[ 'iudex-brutefuzzy-listener' ] = {
        :address => ctx.address_per_process( 'iudex-brutefuzzy-listener' ),
        :assert  => :receiver,
        :create  => :receiver,
        :delete  => :receiver, #FIXME: No-Op?
        :node    => {
          :type        => :queue,
          'x-bindings' => [ { :exchange => 'iudex-brutefuzzy-response' } ],
          'x-declare'  => {
            'auto-delete' => true,
            :arguments    => {
              'qpid.max_size'    => 200_000,
              'qpid.policy_type' => :ring,
            }
          }
        }
      }

    end
  end

end
