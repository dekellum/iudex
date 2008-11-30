
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
