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


# Base Urls table schema
class BaseUrls < ActiveRecord::Migration
  def self.up
    create_table  'urls',  :id => false do |t|

      t.text      'uhash', :null => false  
      # 23 byte ASCII PRIMARY KEY SHA-1 hash fragment of URL 
      # (Note :limit not useful.)
      
      t.text      'url',   :null => false 
      # Complete normalized url (exactly as used for uhash)

      t.text      'host',  :null => false
      # Normalized host portion of URL

      t.text      'type',  :null => false  
      # FEED, PAGE, ROBOTS, SITEMAP 
      # Potentially speculative (i.e. "PAGE" before visited)
      # FIXME: Or REDIRECT here instead of status?

      t.text      'etag'
      # HTTP ETag header used for subsequent conditional GET
      # Should only be on 200 and related HTTP status, not redirect

      t.timestamp 'last_visit'
      # Time of last visit (and thus last type,status,reason,etc.)
      
      t.text      'status'                     
      # null      : Not yet visited
      # ACCEPT    : Accepted, processed successfully
      # REJECT    : Rejected, processing failed (reason has why)
      # REDIRECT  : Was an HTTP 30x redirect (see referent)
      # TRANSIENT : Last/first visit failed with transient error, retry
      # ACCEPT_TRANSIENT : Last visit failed with transient error, use prior visit

      t.text      'reason'
      # null      : None
      #  999      : HTTP status code     
      # DUPE      : Duplicate of referent
      # rejection filter (intended as key)

      t.text      'referent'
      # null      : None
      # uhash of url this is refering to
      # (includes status:REDIRECT, reason:DUPE, etc.)

      t.text      'referer'
      # null      : None
      # uhash of url this was refered from. (i.e. the feed URL)

    end

    execute "ALTER TABLE urls ADD PRIMARY KEY (uhash)"
    add_index 'urls', [ 'host' ]
  end

  def self.down
    drop_table 'urls'
  end
end
