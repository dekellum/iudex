#--
# Copyright (c) 2008-2012 David Kellum
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
require 'iudex-da/config'
require 'active_record'
require 'hooker'

module Iudex::DA

  def self.setup
    log = RJack::SLF4J[ "iudex.da.ActiveRecord" ]
    conf = Hooker.merge( [ :iudex, :connect_props ], CONFIG )
    log.info { "Connecting: #{ conf.inspect }" }

    ActiveRecord::Base.logger = log
    ActiveRecord::Base.establish_connection( conf )
  end

  setup #FIXME: Require explicit setup for use?

  def migrate( target_version = nil )
    base = File.join( LIB_DIR, '..', '..', 'db' )
    paths = [ base ]

    profiles = Hooker.apply( [ :iudex, :migration_profiles ], [] )

    paths += profiles.compact.map do |p|
      p = p.to_s
      if p =~ %r{^/}
        p
      else
        File.join( base, p )
      end
    end

    pattern = if paths.size > 1
                '{' + paths.join( ',' ) + '}'
              else
                paths.first
              end

    ActiveRecord::Migrator.migrate( pattern, target_version )
  end

  module_function :migrate

  class Url < ActiveRecord::Base
    set_primary_key :uhash
    set_inheritance_column :object_type # since "type" used already
  end

end
