#!/opt/bin/jruby

require 'database'

ActiveRecord::Migrator.migrate( File.dirname(__FILE__) + '/db',
                                ARGV[0] && ARGV[0].to_i )
