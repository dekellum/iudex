# -*- ruby -*-

$LOAD_PATH << './lib'
require 'brute-fuzzy/base'

require 'rubygems'
gem     'rjack-tarpit', '~> 1.1'
require 'rjack-tarpit'

t = RJack::TarPit.new( 'brute-fuzzy',
                       BruteFuzzy::VERSION,
                       :no_assembly )

t.specify do |h|
  h.developer( "David Kellum", "dek-oss@gravitext.com" )
  h.extra_deps += [ [ 'gravitext-util', '>= 1.3.2' ] ]
end

file 'Manifest.txt' => [ 'lib/brute-fuzzy.rb' ]

t.define_tasks
