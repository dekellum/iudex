#--
# Copyright (c) 2010 David Kellum
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

require 'iudex-html/base.rb'

require 'java'
require 'gravitext-xmlprod'
require 'rjack-nekohtml'
require 'iudex-filter'
require 'iudex-core'

module Iudex
  module HTML
    require "iudex-html/iudex-html-#{VERSION}.jar"

    import 'iudex.html.HTMLKeys'

    module Filters
      import 'iudex.html.filters.HTMLParseFilter'
      import 'iudex.html.filters.HTMLTreeFilter'
    end

    module Tree
      import 'iudex.html.tree.HTMLTreeKeys'
      import 'iudex.html.tree.TreeFilterChain'

      module Filters
        import 'iudex.html.tree.filters.CharactersNormalizer'
        import 'iudex.html.tree.filters.MetaSkipFilter'
        import 'iudex.html.tree.filters.WordCounter'
        import 'iudex.html.tree.filters.WordyCounter'
      end
    end

  end
end
