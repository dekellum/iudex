# -*- ruby -*- coding: utf-8 -*-

gems = %w[ iudex-filter iudex-http
           iudex-barc iudex-core iudex-httpclient-3
           iudex-da iudex-rome
           iudex-worker ]

desc "Run multi['task1 tasks2'] tasks over all sub gems"
task( :multi, :subtasks ) do |t,args|
  subtasks = args.subtasks.split
  gems.each do |dir|
    Dir.chdir( dir ) do
      puts ">> cd #{dir}"
      sh( "jrake", *subtasks )
    end
  end
end

desc "Run multish['shell command'] over all sub gem dirs"
task( :multish, :subtasks ) do |t,args|
  gems.each do |dir|
    Dir.chdir( dir ) do
      puts ">> cd #{dir}"
      sh( args.subtasks )
    end
  end
end

desc "Aggregated javadocs via Maven"
task :javadoc do
  sh( "mvn javadoc:aggregate" )
end

desc "Lazy dependencies for (re)rdoc tasks"
task :rdocdeps do
  require 'rubygems'
  require 'rdoc'
end

task :rdoc   => [ :rdocdeps ]
task :rerdoc => [ :rdocdeps ]

require 'rake/rdoctask'

Rake::RDocTask.new do |rd|
  rd.main = "README.rdoc"
  rd.rdoc_dir = "doc" # FIXME: www/_site/rdoc?
  rd.title = "Iūdex RDoc"
  rd.options << "--charset=UTF-8"
  rd.rdoc_files.include( "README.rdoc" )
  gems.each do |gem|
    rd.rdoc_files.include( "#{gem}/README.rdoc",
                           "#{gem}/History.rdoc",
                           "#{gem}/lib/**/*.rb" )
  end
end
