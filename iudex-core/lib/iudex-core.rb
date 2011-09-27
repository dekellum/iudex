
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

require 'gravitext-util'
require 'iudex-http'
require 'iudex-filter'
require 'iudex-barc'

require 'iudex-core/base'

require 'iudex-core/config'

require 'java'

module Iudex
  module Core
    require "#{LIB_DIR}/iudex-core-#{VERSION}.jar"

    import 'iudex.core.ContentKeys'
    import 'iudex.core.ContentSource'
    import 'iudex.core.VisitExecutor'
    import 'iudex.core.VisitManager'
    import 'iudex.core.VisitQueueFactory'
    import 'iudex.core.VisitQueue'
    import 'iudex.core.AsyncVisitExecutor'
    import 'iudex.core.VisitURL'

    module Filters
      import 'iudex.core.filters.BARCWriter'
      import 'iudex.core.filters.ContentFetcher'
      import 'iudex.core.filters.DateChangeFilter'
      import 'iudex.core.filters.DefaultFilter'
      import 'iudex.core.filters.FutureDateFilter'
      import 'iudex.core.filters.RedirectHandler'
      import 'iudex.core.filters.Revisitor'
      import 'iudex.core.filters.RLDomainFilter'
      import 'iudex.core.filters.TextCtrlWSFilter'
      import 'iudex.core.filters.UHashMDCSetter'
    end

  end
end

require 'iudex-core/mojibake'
