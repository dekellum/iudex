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

require 'rjack-slf4j'
require 'iudex-da'
require 'active_record'

module Iudex::DA

  @log = SLF4J[ "iudex.da.ActiveRecord" ]
  ActiveRecord::Base.logger = @log

  @log.info { "Connecting: #{CONFIG.inspect}" }
  ActiveRecord::Base.establish_connection( CONFIG )

  def migrate( target_version = nil )
    ActiveRecord::Migrator.migrate( File.join( LIB_DIR, '..', '..', 'db' ),
                                    target_version )
    #FIXME: Support additional migration directories?
  end

  module_function :migrate

  class Url < ActiveRecord::Base
    set_primary_key :uhash
    set_inheritance_column :object_type # since "type" used already
  end

end
