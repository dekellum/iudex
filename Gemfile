# -*- ruby -*-

source :rubygems

gems = %w[ iudex-filter iudex-http iudex-http-test iudex-barc
           iudex-core iudex-httpclient-3 iudex-jetty-httpclient
           iudex-async-httpclient
           iudex-char-detector
           iudex-html iudex-simhash iudex-rome iudex-da
           iudex-worker
           iudex-brutefuzzy-protobuf iudex-brutefuzzy-service
           iudex ]

bdir = File.dirname( __FILE__ )

gems.each do |gname|
  if File.exist? File.join( bdir, gname, gname + ".gemspec" )
    gemspec :path => gname, :name => gname
  end
end
