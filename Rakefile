# -*- ruby -*-

require 'rubygems'

require 'rake/testtask'

$LOAD_PATH << './lib'

file 'lib/brute-fuzzy.jar' => FileList[ 'src/*.java' ] do
  cfiles = FileList[ 'src/*.class' ].map { |cf| File.basename( cf ) }
  sh( 'javac src/*.java' )
  sh( ( [ "jar cvf lib/brute-fuzzy.jar -C src" ] + cfiles ).join( ' ' ) )
end

task :default => [ 'lib/brute-fuzzy.jar' ]

task :test => [ 'lib/brute-fuzzy.jar' ]
Rake::TestTask.new

