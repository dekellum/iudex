# -*- ruby -*-

source :rubygems

gems = %w[ iudex-filter iudex-http iudex-http-test
           iudex-barc
           iudex-core
           iudex-httpclient-3 iudex-jetty-httpclient iudex-async-httpclient
           iudex-html iudex-char-detector
           iudex-simhash iudex-rome iudex-da
           iudex-worker
           iudex-brutefuzzy-protobuf
           iudex-brutefuzzy-service
           iudex ]

gems.each do |gname|
  gemspec :path => gname, :name => gname
end
