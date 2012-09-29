#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

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

require File.join( File.dirname( __FILE__ ), "setup" )

require 'iudex-core'
require 'iudex-da/orm'

class TestPollWork < MiniTest::Unit::TestCase
  include Iudex::DA
  import 'iudex.core.VisitURL'

  def setup
    ORM::Url.delete

    domains = [ 'foo.org', 'other.net', 'gravitext.com', 'one.at' ]
    count = 0
    domains.each do |domain|
      (5..15).each do |val|
        url = ORM::Url.create do |u|
          u.priority = ( val.to_f / 10.0 ) + (count.to_f / 50.0)
          vurl = VisitURL.normalize( "http://#{domain}/#{u.priority}" )
          u.type = "FEED"
          u.domain = vurl.domain
          u.url = vurl.to_s
          u.uhash = vurl.uhash
          u.next_visit_after = Time.now
          count += 1
        end
      end
    end
  end

  def teardown
    ORM::Url.delete
  end

  # Query to get new work, with limits on work per domain, and total
  # work (in descending piority order)
  def test_poll
    query = <<END
SELECT url, domain, type, priority
FROM ( SELECT *, row_number() OVER ( ORDER BY priority DESC ) as ppos
       FROM ( SELECT *, row_number() OVER ( PARTITION BY domain
                                            ORDER BY priority DESC ) AS hpos
              FROM urls
              WHERE next_visit_after <= now() ) AS subh
       WHERE hpos <= ? ) AS subp
WHERE ppos <= ?
ORDER BY domain, priority DESC
END
    res = ORM::Url.where( query, 5, 18 )

    def check_domain_subset( bydomain )
      assert( bydomain.length <= 5 )
      bydomain.each_cons(2) { |p,n| assert( p.priority >= n.priority ) }
    end

    assert( res.count <= 18 )
    bydomain = []
    res.each do |u|
      if bydomain.empty? || bydomain.last.domain == u.domain
        bydomain << u
      else
        check_domain_subset( bydomain )
        bydomain = []
      end
    end
    check_domain_subset( bydomain ) unless bydomain.empty?

  end

  def test_insert

    ORM.db.transaction do
      sql = <<END
CREATE TEMPORARY TABLE mod_urls
  ( uhash text,
    url text,
    domain text )
END
    # ON COMMIT DROP;

      ORM.db.run( sql )

      # Url.set_table_name "mod_urls"

      count = ( 11 * 2 )
      (5..20).each do |val|
#        url = Url.create! do |u|
        priority = ( val.to_f / 10.0 ) + (count.to_f / 50.0)
          # u.priority =
          # u.type = "FEEDX"
        vurl = VisitURL.normalize( "http://gravitext.com/#{priority}" )

        sql = "INSERT into mod_urls VALUES ('%s','%s','%s')" %
          [ vurl.uhash, vurl.to_s, vurl.domain ]
        ORM.db.run( sql )
          # u.next_visit_after = Time.now
        count += 1
      end
      insert_query = <<END
INSERT INTO urls (uhash,url,domain,type,priority)
  ( SELECT uhash,url,domain,'FEEDX',4.78 FROM mod_urls
    WHERE uhash NOT IN ( SELECT uhash FROM urls ) )
END
      ORM::db.run( insert_query )

      ORM::db.run( "DROP TABLE mod_urls" )

      # Url.set_table_name "urls"
    end

  end

end
