# http://api.rubyonrails.org/classes/ActiveRecord/Migration.html

class AddPriority < ActiveRecord::Migration
  def self.up
    add_column 'urls',  'priority', :float, :null => false, :default => 0.0
    add_index  'urls',[ 'priority' ], :name => 'index_urls_on_priority'
  end

  def self.down
    remove_index  'urls', 'index_urls_on_priority'
    remove_column 'urls', 'priority'
  end
end
