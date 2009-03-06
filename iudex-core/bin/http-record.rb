#!/usr/bin/env jruby

require 'rubygems'
require 'logback'

Logback.configure do
  console = Logback::ConsoleAppender.new do |a|
    a.target = "System.err"
    a.layout = Logback::PatternLayout.new do |p|
      p.pattern = "%-5r %-5level %logger{35} - %msg %ex%n"
    end
  end
  Logback.root.add_appender( console )
  Logback.root.level = Logback::INFO
  #Logback[ "org.apache.commons.httpclient" ].level = Logback::INFO
  #Logback[ "iudex" ].level = Logback::DEBUG
end

$LOAD_PATH.unshift File.join( File.dirname(__FILE__), "..", "lib" )

require 'iudex-core'

import 'iudex.http.httpclient3.HTTPClient3'
import 'iudex.http.barc.BARCResponseHandler'
import 'iudex.barc.BARCFile'

hmanager = HC::HTTPClient::ManagerFacade.new
hmanager.start

hclient = HTTPClient3.new( hmanager.client )

barc_file = BARCFile.new( java.io.File.new( './record.barc' ) ) #FIXME: param
barc_file.truncate #FIXME: Optional

handler = BARCResponseHandler.new( barc_file )
handler.do_compress = false #FIXME: Option

hsession = hclient.createSession;
hsession.url = 'http://gravitext.com/blog' #FIXME: param

hclient.request( hsession, handler )

hmanager.shutdown
