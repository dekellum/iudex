#--
# Copyright (c) 2008-2014 David Kellum
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

Sequel.migration do
  change do

    # The main/only urls table, in matching a default-setup iudex
    # 1.1.0-1.2.1.
    create_table( :urls ) do

      twtz = "timestamp with time zone"
      now  = Sequel::CURRENT_TIMESTAMP

      String    :uhash,            :null => false
      # 23 byte ASCII PRIMARY KEY SHA-1 hash fragment of URL

      String    :url,              :null => false
      # Complete normalized url (exactly as used for uhash)

      String    :domain,           :null => false
      # Registration level domain from url host

      String    :type,             :null => false
      # FEED, PAGE, ROBOTS, SITEMAP
      # Potentially speculative (i.e. "PAGE" before visited)
      # FIXME: Or REDIRECT here instead of status?

      String    :etag
      # HTTP ETag header used for subsequent conditional GET
      # Should only be on 200 and related HTTP status, not redirect

      DateTime  :last_visit,       :type => twtz
      # Time of last visit (and thus last type,status,reason,etc.)

      Integer   :status
      # HTTP status code or special (negative) status mapping
      # null     : Not yet visited
      #  -1      : Connection Failed
      # 4xx      : Permanent Failures
      # 5xx      : Transient server error
      # 200      : Success
      # 304      : Not Modified
      # 301,302  : Redirect
      # http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html

      TrueClass :pass
      # null     : Not yet processed (i.e. visit failed)
      # false    : Rejected by processing (for reason), DELETE required
      # true     : Fully Processed

      String    :reason
      # null      : None
      # DUPE      : Duplicate of referent
      # rejection filter (intended as key)

      String    :referent
      # null      : None
      # uhash of url this is refering to
      # (includes status:REDIRECT, reason:DUPE, etc.)

      String    :referer
      # null      : None
      # uhash of url this was refered from. (i.e. the feed URL)

      String    :title
      # PAGE,FEED title

      DateTime  :ref_pub_date,     :type => twtz
      # (Latest) published date as provided from feed (may be ahead of
      # or set before pub_date, below).

      DateTime  :pub_date,         :type => twtz
      # (Latest) published date as processed

      String    :summary
      # (Feed) summary

      String    :content
      # (Feed) content

      Float     :priority,         :type => "real", :default => 0.0, :null => false
      # Prioritization of next visit, range -INF,+INF

      DateTime  :next_visit_after, :type => twtz,   :default => now
      # null: never visit (terminal result)
      # Don't visit again before the specified date.

      Integer   :cache_file
      # 32-bit file number

      Bignum    :cache_file_offset
      # 64-bit byte offset within file

      DateTime  :created_at,       :type => twtz,   :default => now
      # When inserted

      Bignum    :simhash
      # A simhash signature as a signed 8-byte long (should be
      # compatible with java long).

      primary_key [ :uhash ]
    end
  end
end
