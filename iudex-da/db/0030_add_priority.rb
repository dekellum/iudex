#--
# Copyright (C) 2008-2009 David Kellum
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

class AddPriority < ActiveRecord::Migration

  def self.up
    add_column    'urls',   'priority',  :float, :null => false, :default => 0.0
    # Prioritization of next visit, range -INF,+INF

    add_index     'urls', [ 'priority' ] 
  end

  def self.down
    remove_index  'urls',   'priority'
    remove_column 'urls',   'priority'
  end

end
