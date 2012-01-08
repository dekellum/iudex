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

class AddSimhash < ActiveRecord::Migration

  def self.up
    add_column( 'urls', 'simhash', :integer, :limit => 8 )
    # A simhash signature as a signed 8-byte long (should be
    # compatible with java long).

    add_index( 'urls', [ 'simhash' ] )
    # And its index
  end

  def self.down
    remove_index( 'urls', 'simhash'  )
    remove_column( 'urls', 'simhash' )
  end

end
