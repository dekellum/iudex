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


# Active record setup for crawler_test

require 'rubygems'
require 'slf4j'
require 'logback'
require 'activerecord'

database = { 'host'     => 'localhost',
             'adapter'  => 'jdbcpostgresql',
             'database' => 'crawler_test',
             'username' => 'david' }

# ActiveRecord::Base.default_timezone = :utc
# ActiveRecord::Base.connection.query_cache_enabled = false

ActiveRecord::Base.establish_connection( database )
ActiveRecord::Base.logger = SLF4J::Logger.new( "state_db" )

Logback[ "state_db" ].level = Logback::DEBUG

class Url < ActiveRecord::Base
  set_primary_key :uhash
  self.inheritance_column = "object_type" 
end
