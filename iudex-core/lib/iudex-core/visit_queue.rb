#--
# Copyright (c) 2008-2015 David Kellum
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

module Iudex::Core

  # Configuration extensions for Java::iudex.core.VisitQueue.
  class VisitQueue

    # Configure defaults, a specific domain or domain,type pair via an
    # options Hash.
    #
    # ==== Options
    #
    # :domain:: Registration level domain String. If not specified,
    #           :type is ignored and other options apply as general
    #           defaults for all (otherwise un-configured
    #           domains/types).
    #
    # :type:: An optional type (i.e. PAGE). If specified, this
    #         :domain,:type pair will be given its own HostQueue with
    #         other the options applying exclusively to it.
    #
    # :rate:: Target maximum rate of crawl as a Float requests/second
    #         for this :domain(,:type) or the default for any not
    #         otherwise configured. Resource limits including :cons
    #         and HTTP client connections may further inhibit rate
    #         below this value. (Initial default is 2.0 req/second)
    #
    # :delay:: Alternative inverse to :rate as Integer milliseconds to
    #          delay between scheduling visits. If specifies, takes
    #          precedence over rate.
    #
    # :cons:: Maximum number of concurrent requests for this
    #         :domain(,:type) or the default for any not otherwise
    #         configured. Note that the HTTP clients have their own
    #         per *host:port* destination connection limit which
    #         should generally be set higher than this value.
    #         (Initial default: 1)
    #
    def config( opts = {} )

      if opts[ :domain ]
        opts = { :rate => delay_to_rate( default_min_host_delay ),
                 :cons => default_max_access_per_host }.merge( opts )
        configure_host( opts[ :domain ],
                        opts[ :type ], # includes nil
                        opts[ :delay ] || rate_to_delay( opts[ :rate ] ),
                        opts[ :cons ] )
      else
        if opts[ :rate ]
          self.default_min_host_delay = rate_to_delay( opts[ :rate ] )
        end
        self.default_min_host_delay = opts[ :delay ] if opts[ :delay ]
        self.default_max_access_per_host = opts[ :cons ] if opts[ :cons ]
      end

    end

    private

    def rate_to_delay( r )
      ( 1_000.0 / r ).round
    end

    def delay_to_rate( d )
      ( 1_000.0 / d )
    end

  end

end
