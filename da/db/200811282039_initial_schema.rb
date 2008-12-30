# http://api.rubyonrails.org/classes/ActiveRecord/Migration.html

class InitialSchema < ActiveRecord::Migration
  def self.up
    create_table 'urls', :id => false do |t|
      t.text 'uhash',  :null => false 
      t.text 'url',    :null => false 
      t.text 'host',   :null => false
      t.text 'type',   :null => false
      t.datetime 'last_visit'
    end
    execute "ALTER TABLE urls ADD PRIMARY KEY (uhash)"
    add_index 'urls', [ 'host' ], :name => 'index_urls_on_host'
  end

  def self.down
    drop_table 'urls'
  end
end
