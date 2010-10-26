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

require 'iudex-simhash'

module Iudex
  module SimHash
    module Filters
      module FactoryHelper
        include Iudex::Core
        include Iudex::HTML

        DEFAULT_WORDS = File.join( File.dirname( __FILE__ ), '..', '..',
                                   'config', 'stopwords.en' )

        def simhash_stopwords( wfile = DEFAULT_WORDS )
          words = open( wfile ) { |fin| fin.readlines }
          words.map! { |w| w.strip }

          Gen::StopWordSet.new( words )
        end

        Element = Java::com.gravitext.xml.tree.Element

        def simhash_generator( input = :simhash_generator_inputs,
                               stopwords = simhash_stopwords )

          inputs = send( input ).map { |r| r.to_a }.map do | key, ratio |
            i = if( key.value_type == Element.java_class )
                  SimHashGenerator::Input.forTree( key )
                else
                  SimHashGenerator::Input.forText( key )
                end
            i.wordy_ratio = ratio if ratio
            i
          end

          SimHashGenerator.new( inputs, stopwords )
        end

        def simhash_generator_inputs
          [ [ ContentKeys::TITLE ],
            [ HTMLKeys::CONTENT_TREE, 0.30 ] ]
        end

      end
    end
  end
end