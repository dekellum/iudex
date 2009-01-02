
require 'rubygems'
require 'rrome/base'

module RRome
  Dir.glob( File.join( RROME_DIR, '*.jar' ) ).each { |jar| require jar }
end
