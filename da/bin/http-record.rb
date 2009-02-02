#!/usr/bin/env jruby

$LOAD_PATH.unshift File.join( File.dirname(__FILE__), "..", "lib" )

require 'rubygems'
require 'logback'
require 'slf4j/jcl-over-slf4j' 
require 'httpclient'     #FIXME: Rename
require 'iudex'
require 'gravitext-util' #FIXME: Iudex require

import 'iudex.http.httpclient3.HTTPClient3'
import 'iudex.http.barc.BARCResponseHandler'
import 'iudex.barc.BARCFile'

hmanager = HTTPClient::ManagerFacade.new
hmanager.start

hclient = HTTPClient3.new( hmanager.client )

barc_file = BARCFile.new( java.io.File.new( './record.barc' ) ) #FIXME: param
barc_file.truncate #FIXME: Optional

handler = BARCResponseHandler.new( barc_file )
handler.do_compress = false #FIXME: Option

hsession = hclient.createSession;
hsession.url = 'http://gravitext.com/blog/' #FIXME: param

hclient.request( hsession, handler )

hmanager.shutdown
