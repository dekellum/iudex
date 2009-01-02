# -*- ruby -*-

require 'rubygems'

ENV['NODOT'] = "no thank you"
require 'hoe'

$LOAD_PATH << './lib'
# require 'rrome/base'

RRVERSION = "1.0" #FIXME

ASSEMBLY = "target/rrome-#{RRVERSION}-bin.dir"

desc "Update the Manifest with actual jars"
task :manifest => [ ASSEMBLY ] do
  out = File.new( 'Manifest.txt', 'w' ) 
  begin
    out.write <<END
History.txt
Manifest.txt
README.txt
Rakefile
pom.xml
assembly.xml
END
    out.puts( Dir.glob( File.join( ASSEMBLY, '*.jar' ) ).map do |jar|
                File.join( 'lib', 'rrome', File.basename( jar ) )
              end )
  ensure
    out.close
  end
end

file ASSEMBLY => [ 'pom.xml', 'assembly.xml' ] do
  sh( 'mvn package' ) rescue abort('package step failed; ' +
                                   'try running mvn install in evrinid-parent')
end

JAR_FILES = File.new( 'Manifest.txt' ).readlines.map { |f| f.strip }\
.select { |f| f =~ /\.jar$/ }

JARS = JAR_FILES.map { |f| File.basename( f.strip ) }

JARS.each do |jar|
  file "lib/rrome/#{jar}" => [ ASSEMBLY ] do
    cp_r( File.join( ASSEMBLY, jar ), 'lib/rrome' )
  end
end

[ :gem, :test ].each { |t| task t => JAR_FILES }

task :mvn_clean do
  rm_f( JAR_FILES )
  sh( 'mvn clean' )
end
task :clean => :mvn_clean 

hoe = Hoe.new( 'rrome', RRVERSION ) do |p|
  p.developer( "David Kellum", "david@gravitext.com" )
  p.extra_deps << [ 'logback', '~> 0.9'  ]
  p.rdoc_pattern = /^(lib.*\.(rb|txt))|[^\/]*\.txt$/
end
