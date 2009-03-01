
require 'iudex-core/base'

module Iudex
  Dir.glob( File.join( IUDEX_CORE_DIR, '*.jar' ) ).each { |jar| require jar }
end
