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

# ActiveRecord maps :timestamp to pg timestamp (without time zone). Add the timezone
# back in a reversable way.
# So far looks like just a cosmetic improvements for psql.
class TimestampsWithTimezone < ActiveRecord::Migration

  STAMPS = [ :ref_pub_date, :pub_date, :next_visit_after ]
  # FIXME: Find these instead?

  def self.up
    STAMPS.each do |col|
     execute "ALTER TABLE urls ALTER COLUMN #{col} TYPE timestamp with time zone"
    end
  end

  def self.down
    STAMPS.each do |col|
     execute "ALTER TABLE urls ALTER COLUMN #{col} TYPE timestamp"
    end
  end

end
