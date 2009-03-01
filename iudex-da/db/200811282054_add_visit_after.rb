# http://api.rubyonrails.org/classes/ActiveRecord/Migration.html

class AddVisitAfter < ActiveRecord::Migration
  def self.up
    add_column 'urls', 'next_visit_after', :datetime
    add_index( 'urls', [ 'next_visit_after' ], 
               :name => 'index_urls_on_next_visit_after' )
  end

  def self.down
    remove_index 'urls', 'index_urls_on_next_visit_after'
    remove_column 'urls', 'next_visit_after'
  end
end
