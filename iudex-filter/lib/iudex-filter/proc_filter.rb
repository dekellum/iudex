#--
# Copyright (c) 2008-2012 David Kellum
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

  # Short hand for ProcFilter.new
  def fltr( &block )
    ProcFilter.new( caller.first, &block )
  end

  class ProcFilter < FilterBase

    # New ProcFilter using block as implmentation of
    # Filter.filter( map ).  The created filter will only return false
    # (reject map, stop chain) if the block returns the :reject
    # symbol.
    def initialize( clr = nil, &block )
      @block = block

      clr ||= caller.first
      clr = clr.split( /:/ )
      @description = [ File.basename( clr[0], ".rb" ), clr[1].to_i ]
      #FIXME: When ruby 1.9, can use Proc.source_location instead.
    end

    def describe
      @description
    end

    def filter( map )
      ( @block.call( map ) != :reject )
    end
  end

end
