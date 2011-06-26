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
require 'iudex-filter/key_helper'

module Iudex
  module HTML
    module Filters
      module FactoryHelper
        include Iudex::Core # ContentSource
        include Iudex::HTML::Tree
        include Iudex::HTML::Tree::Filters

        # Create html parse and clean filters
        # Expected usage:
        #   PAGE: html_clean_filters( :source  )
        #   FEED: html_clean_filters( :title   )
        #   FEED: html_clean_filters( :summary )
        #   FEED: html_clean_filters( :content )
        #
        def html_clean_filters( src_key, tree_key = nil )

          tree_key = "#{src_key}_tree".to_sym unless tree_key
          src_key, tree_key = src_key.to_k, tree_key.to_k

          filters = []
          filters << html_parse_filter( src_key, tree_key )

          #FIXME: PAGE: filters << TitleExtractor.new, or after?

          # FIXME: if src is text, last filter
          # filters << TextCtrlWSFilter.new( ContentKeys::TITLE )

          tfc = TreeFilterChain.new( html_tree_filters )

          filters << HTMLTreeFilter.new( tree_key, tfc,
                                         HTMLTreeFilter::Order::DEPTH_FIRST )

          #FIXME: First block extractor back to text key?

          filters
        end

        def html_tree_filters
          [ XmpToPreConverter.new,    # Before CharactersNormalizer
            CSSDisplayFilter.new,     # Before AttributeCleaner
            AttributeCleaner.new,
            MojiBakeCleaner.new,
            CharactersNormalizer.new,
            EmptyInlineRemover.new,   # Depth
            WordCounter.new,          # Depth; only for count deps?
            WordyCounter.new ]        # Depth; only with cleaners/simhash?
        end

        def html_parse_filter( src_key, tree_key = nil )

          tree_key = "#{src_key}_tree".to_sym unless tree_key
          src_key, tree_key = src_key.to_k, tree_key.to_k

          if( src_key.value_type == ContentSource.java_class )
            HTMLParseFilter.new( src_key, nil, tree_key )
          else
            HTMLParseFilter.new( src_key, tree_key )
          end
        end

        # Expected usage:
        #   FEED: html_write_filter( :summary )
        def html_write_filter( key1, key2 = nil )

          tree_key, out_key = if key2
                                [ key1, key2 ]
                              else
                                [ "#{key1}_tree".to_sym, key1 ]
                              end

          HTMLWriteFilter.new( tree_key.to_k, out_key.to_k )
        end
      end
    end
  end
end
