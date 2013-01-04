#--
# Copyright (c) 2008-2013 David Kellum
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

require 'iudex-core'
require 'gravitext-xmlprod'
require 'gravitext-xmlprod/extensions'
require 'rjack-nekohtml'

require 'iudex-html/base'

require 'java'

module Iudex
  module HTML
    require "iudex-html/iudex-html-#{VERSION}.jar"

    import 'iudex.html.HTMLKeys'

    module Filters
      import 'iudex.html.filters.ExtractFilter'
      import 'iudex.html.filters.HTMLParseFilter'
      import 'iudex.html.filters.HTMLTreeFilter'
      import 'iudex.html.filters.HTMLWriteFilter'
      import 'iudex.html.filters.TitleExtractor'
    end

    module Tree
      import 'iudex.html.tree.HTMLTreeKeys'
      import 'iudex.html.tree.TreeFilterChain'
      import 'iudex.html.tree.HTMLStAXConsumer'
      import 'javax.xml.stream.XMLStreamException'

      # Parse the input String using HTMLStAXConsumer, HTMLTag.
      # Raises XMLStreamException on parse error
      def self.parse( input )
        Gravitext::XMLProd::XMLHelper.
          stax_parse_string( input, HTMLStAXConsumer.new )
      end

      module Filters
        import 'iudex.html.tree.filters.AttributeCleaner'
        import 'iudex.html.tree.filters.CSSDisplayFilter'
        import 'iudex.html.tree.filters.CharactersNormalizer'
        import 'iudex.html.tree.filters.EmptyInlineRemover'
        import 'iudex.html.tree.filters.MetaSkipFilter'
        import 'iudex.html.tree.filters.MojiBakeCleaner'
        import 'iudex.html.tree.filters.WordCounter'
        import 'iudex.html.tree.filters.WordyCounter'
        import 'iudex.html.tree.filters.XmpToPreConverter'

        # Re-open iudex.html.tree.filter.MojiBakeCleaner to add config file
        # based initialization.
        class MojiBakeCleaner
          include Iudex::Core

          # Alt constructor taking a configuration file in `mojibake
          # -t` format.
          def initialize( config_file = :default )
            args = Array( config_file ) - [ :default ]
            super( *MojiBake.load_config( *args ) )
          end
        end

      end
    end

  end
end
