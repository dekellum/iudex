#--
# Copyright (c) 2008-2015 David Kellum
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

require 'rjack-slf4j'
require 'gravitext-util'

require 'iudex-http/base'

require 'java'

module Iudex
  module HTTP
    require "#{LIB_DIR}/iudex-http-#{VERSION}.jar"

    import "iudex.http.BaseResponseHandler"
    import "iudex.http.ContentType"
    import "iudex.http.ContentTypeSet"
    import "iudex.http.HTTPClient"
    import "iudex.http.HTTPKeys"
    import "iudex.http.HTTPSession"
    import "iudex.http.Header"
    import "iudex.http.Headers"
    import "iudex.http.ResponseHandler"
    import "iudex.http.RequestContent"

  end
end
