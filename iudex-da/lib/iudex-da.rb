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

require 'rjack-commons-dbcp'
require 'rjack-commons-dbutils'

require 'iudex-da/base'
require 'iudex-da/config'

require 'java'

module Iudex
  module DA

    require "#{LIB_DIR}/iudex-da-#{VERSION}.jar"

    import 'iudex.da.DAKeys'
    import 'iudex.da.ContentMapper'
    import 'iudex.da.ContentReader'

    module Filters
      import 'iudex.da.filters.UpdateFilter'
      import 'iudex.da.filters.ReadFilter'
    end

  end
end

require 'iudex-da/work_poller.rb'
