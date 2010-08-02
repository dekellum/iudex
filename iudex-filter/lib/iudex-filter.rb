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

require 'gravitext-util'
require 'rjack-slf4j'
require 'iudex-filter/base'

module Iudex
  module Filter
    require "#{LIB_DIR}/iudex-filter-#{VERSION}.jar"

    import 'iudex.filter.Filter'
    import 'iudex.filter.FilterContainer'
    import 'iudex.filter.Described'
    import 'iudex.filter.Named'
    import 'iudex.filter.NoOpFilter'

    module Core
      import 'iudex.filter.core.ByFilterReporter'
      import 'iudex.filter.core.Copier'
      import 'iudex.filter.core.FilterChain'
      import 'iudex.filter.core.FilterIndex'
      import 'iudex.filter.core.ListenerChain'
      import 'iudex.filter.core.LogListener'
      import 'iudex.filter.core.MDCUnsetter'
      import 'iudex.filter.core.Selector'
      import 'iudex.filter.core.SummaryReporter'
      import 'iudex.filter.core.Switch'
    end
  end
end

require 'iudex-filter/filter_base'
