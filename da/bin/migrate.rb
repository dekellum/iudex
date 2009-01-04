#!/opt/bin/jruby

$LOAD_PATH.unshift File.join( File.dirname(__FILE__), "..", "lib" )

require 'iudex/ar'

ActiveRecord::Migrator.migrate( File.dirname(__FILE__) + '/../db',
                                ARGV[0] && ARGV[0].to_i )
