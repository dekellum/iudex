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

# http://api.rubyonrails.org/classes/ActiveRecord/Migration.html
# http://api.rubyonrails.org/classes/ActiveRecord/ConnectionAdapters/TableDefinition.html

class InitialSchema < ActiveRecord::Migration
  def self.up
    create_table  'urls', :id => false do |t|
      t.text      'uhash',     :null => false  # ASCII 23B, but no :limit needed
      t.text      'url',       :null => false 
      t.text      'host',      :null => false
      t.text      'type',      :null => false
      t.timestamp 'last_visit'
    end
    execute "ALTER TABLE urls ADD PRIMARY KEY (uhash)"
    add_index 'urls', [ 'host' ]
  end

  def self.down
    drop_table 'urls'
  end
end
