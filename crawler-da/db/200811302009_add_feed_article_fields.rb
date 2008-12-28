# http://api.rubyonrails.org/classes/ActiveRecord/Migration.html

class AddFeedArticleFields < ActiveRecord::Migration
  def self.up
    add_column 'urls',  'feed_title', :text
    add_column 'urls',  'publish_date', :datetime
  end

  def self.down
    remove_column 'urls', 'priority'
    remove_column 'urls', 'publish_date'
  end
end
