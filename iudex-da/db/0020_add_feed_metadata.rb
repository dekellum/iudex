#--
# Copyright (c) 2008-2012 David Kellum
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

class AddFeedMetadata < ActiveRecord::Migration

  def self.up
    add_column( 'urls', 'title', :text )
    # PAGE,FEED title

    add_column( 'urls', 'ref_pub_date', 'timestamp with time zone' )
    # (Latest) published date as provided from feed (may be ahead of
    # or set before pub_date, below).

    add_column( 'urls', 'pub_date', 'timestamp with time zone' )
    # (Latest) published date as processed
  end

  def self.down
    remove_column( 'urls', 'title' )
    remove_column( 'urls', 'ref_pub_date' )
    remove_column( 'urls', 'pub_date' )
  end

end
