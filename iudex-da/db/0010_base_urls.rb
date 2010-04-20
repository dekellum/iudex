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

# Base Urls table schema
class BaseUrls < ActiveRecord::Migration
  def self.up
    create_table( 'urls', :id => false ) {}

    add_column( 'urls', 'uhash', :text, :null => false )
    # 23 byte ASCII PRIMARY KEY SHA-1 hash fragment of URL
    # (Note :limit not useful.)

    add_column( 'urls', 'url', :text,  :null => false )
    # Complete normalized url (exactly as used for uhash)

    add_column( 'urls', 'host', :text, :null => false )
    # Normalized host portion of URL

    add_column( 'urls', 'type', :text, :null => false )
    # FEED, PAGE, ROBOTS, SITEMAP
    # Potentially speculative (i.e. "PAGE" before visited)
    # FIXME: Or REDIRECT here instead of status?

    add_column( 'urls', 'etag', :text )
    # HTTP ETag header used for subsequent conditional GET
    # Should only be on 200 and related HTTP status, not redirect

    add_column( 'urls', 'last_visit', 'timestamp with time zone' )
    # Time of last visit (and thus last type,status,reason,etc.)

    add_column( 'urls', 'status', :integer )
    # HTTP status code or special (negative) status mapping
    # null     : Not yet visited
    #  -1      : Connection Failed
    # 4xx      : Permanent Failures
    # 5xx      : Transient server error
    # 200      : Success
    # 304      : Not Modified
    # 301,302  : Redirect
    # http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
    # Compare to: http://crawler.archive.org/articles/user_manual/glossary.html#statuscodes

    add_column( 'urls', 'pass', :boolean )
    # null     : Not yet processed (i.e. visit failed)
    # false    : Rejected by processing (for reason), DELETE required
    # true     : Fully Processed

    add_column( 'urls', 'reason', :text )
    # null      : None
    # DUPE      : Duplicate of referent
    # rejection filter (intended as key)

    add_column( 'urls', 'referent', :text )
    # null      : None
    # uhash of url this is refering to
    # (includes status:REDIRECT, reason:DUPE, etc.)

    add_column( 'urls', 'referer', :text )
    # null      : None
    # uhash of url this was refered from. (i.e. the feed URL)

    execute( "ALTER TABLE urls ADD PRIMARY KEY (uhash)" )
  end

  def self.down
    drop_table 'urls'
  end
end
