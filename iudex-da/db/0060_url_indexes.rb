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

# http://api.rubyonrails.org/classes/ActiveRecord/Migration.html
# http://api.rubyonrails.org/classes/ActiveRecord/ConnectionAdapters/TableDefinition.html

# Indexes for urls table
class UrlIndexes < ActiveRecord::Migration
  def self.up
    # FIXME:  Disabled for now. Which are helpful?

    # add_index( 'urls', [ 'host' ] )
    # Used by (obsolesced) LIMIT/sub-query based work poll

    # add_index( 'urls', [ 'priority' ] )
    # FIXME: Consider partial index, e.g. WHERE next_visit_after IS NOT NULL?
    # FIXME: Consider a combined index 'host', 'priority'?

    # add_index( 'urls', [ 'next_visit_after' ] )
    # Used by (obsolesced) LIMIT/sub-query based work poll
  end

  def self.down
    # remove_index( 'urls', 'host' )
    # remove_index( 'urls', 'priority' )
    # remove_index( 'urls', 'next_visit_after' )
  end
end
