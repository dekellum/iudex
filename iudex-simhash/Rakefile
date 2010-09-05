# -*- ruby -*-

$LOAD_PATH << './lib'
require 'brute-fuzzy/base'

require 'rubygems'
gem     'rjack-tarpit', '~> 1.2.0'
require 'rjack-tarpit'

t = RJack::TarPit.new( 'brute-fuzzy',
                       BruteFuzzy::VERSION,
                       :no_assembly, :java_platform )

t.specify do |h|
  h.developer( "David Kellum", "dek-oss@gravitext.com" )
  h.extra_dev_deps += [ [ 'gravitext-util', '>= 1.3.2' ] ]
end

file 'Manifest.txt' => [ 'pom.xml' ]

task :check_pom_version do
  t.test_line_match( 'pom.xml', /<version>/, /#{t.version}/ )
end
task :check_history_version do
  t.test_line_match( 'History.rdoc', /^==/, / #{t.version} / )
end
task :check_history_date do
  t.test_line_match( 'History.rdoc', /^==/, /\([0-9\-]+\)$/ )
end

task :gem  => [ :check_pom_version, :check_history_version                      ]
task :tag  => [ :check_pom_version, :check_history_version, :check_history_date ]
task :push => [                                             :check_history_date ]

t.define_tasks
