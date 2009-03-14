#!/usr/bin/env jruby

#--
# Copyright (C) 2008-2009 David Kellum
#
# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License.  You
# may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.  See the License for the specific language governing
# permissions and limitations under the License.
#++

$LOAD_PATH.unshift File.join( File.dirname(__FILE__), "..", "lib" )

require 'iudex-core'
require 'iudex-core/console_logback' 
#Logback[ "org.apache.commons.httpclient" ].level = Logback::INFO
#Logback[ "iudex" ].level = Logback::DEBUG

require 'iudex-core/http'

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
