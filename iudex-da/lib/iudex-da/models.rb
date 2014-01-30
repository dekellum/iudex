#--
# Copyright (c) 2008-2014 David Kellum
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

require 'iudex-da/orm'

# Ensure setup has been run, as Sequel::Model needs the database
# connected for schema at model class creation
Iudex::DA::ORM.db

module Iudex::DA::ORM

  # Url model (for urls table). Usage note: ORM::setup must be called
  # before this can be loaded.
  class Url < ::Sequel::Model

    VisitURL = Iudex::Core::VisitURL

    plugin :composition

    composition( :visit_url,
                 :composer => proc { VisitURL.trust( url ) },
                 :decomposer => proc {
                   if v = compositions[ :visit_url ]
                     self.url    = v.url
                     self.uhash  = v.uhash
                     self.domain = v.domain
                   end
                 } )

    def visit_url=( vurl )
      vurl = VisitURL.normalize( vurl ) unless vurl.is_a?( VisitURL )
      super( vurl )
    end

    def self.find_by_url( vurl )
      vurl = VisitURL.normalize( vurl ) unless vurl.is_a?( VisitURL )
      self[ vurl.uhash ]
    end

    # Specifically include type accessors to avoid deprecation warnings for
    # old ruby method.

    def type
      self[ :type ]
    end

    def type=( t )
      self[ :type ] = t
    end

  end

end
