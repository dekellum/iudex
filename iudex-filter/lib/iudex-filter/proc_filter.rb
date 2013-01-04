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

require 'iudex-filter/filter_base'

module Iudex::Filter

  # Short hand for ProcFilter.new( opts, &block )
  def fltr( opts = {}, &block )
    ProcFilter.new( { :caller => caller.first }.merge( opts ), &block )
  end

  # Return a Procfilter using method( map ) identified by method_symbol
  # in self's scope as filter. The default :desc is [ method_symbol ].
  def fltr_method( method_symbol, opts = {} )
    ProcFilter.new( { :desc => [ method_symbol ] }.merge( opts ),
                    &( method method_symbol ) )
  end

  # A filter wrapping a Ruby Proc.
  class ProcFilter < FilterBase

    # Use block as implementation of filter(map).  The filter will
    # only return false (reject map, stop chain) if the block returns
    # the :reject symbol. All other return values are ignored,
    # including "false".
    #
    # === Options
    #
    # Passing a String for opts is interpreted as :caller (deprecated
    # as of 1.3.0)
    #
    # :caller:: String from which to compose the description. This is
    #           used under Ruby 1.8 only, obtained via
    #           block.source_location in 1.9.
    #
    # :desc:: Array<~to_s> description (overrides caller)
    #
    def initialize( opts = {}, &block )
      @block = block

      opts = { :caller => opts } if opts.is_a?( String )
      @description = Array( opts[ :desc ] ).compact

      if @description.empty?
        if @block.respond_to?( :source_location ) # ruby 1.9
          clr = @block.source_location
        else                                      # ruby 1.8
          clr = opts[ :caller ] || caller.first
          clr = clr.split( /:/ )
        end

        @description = [ File.basename( clr[0], ".rb" ), clr[1].to_i ]
      end

    end

    def describe
      @description
    end

    # Calls initialized block, returning true if it returns anything
    # but :reject
    def filter( map )
      ( @block.call( map ) != :reject )
    end
  end

end
