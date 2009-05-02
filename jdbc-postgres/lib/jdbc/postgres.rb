module Jdbc
  module Postgres
    VERSION = "8.3" 
  end
end
if RUBY_PLATFORM =~ /java/
  require "postgresql-#{Jdbc::Postgres::VERSION}-604.jdbc4.jar"
else
  warn "jdbc-postgres is only for use with JRuby"
end
