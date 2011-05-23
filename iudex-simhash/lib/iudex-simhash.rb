#--
# Copyright (c) 2010-2011 David Kellum
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

require 'iudex-html'

require 'iudex-simhash/base.rb'

require 'java'

module Iudex
  module SimHash

    require "iudex-simhash/iudex-simhash-#{VERSION}.jar"

    import 'iudex.simhash.SimHashKeys'

    module BruteFuzzy
      import 'iudex.simhash.brutefuzzy.BruteFuzzy'
      import 'iudex.simhash.brutefuzzy.FuzzyList64'
      import 'iudex.simhash.brutefuzzy.FuzzyTree64'
    end

    module Filters
      import 'iudex.simhash.filters.SimHashGenerator'
    end

    module Gen
      import 'iudex.simhash.gen.StopWordSet'
    end
  end
end
