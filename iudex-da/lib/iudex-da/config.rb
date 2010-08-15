
#--
# Copyright (c) 2008-2010 David Kellum
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

module Iudex

  module DA
    # Database connection configuration for both ActiveRecord
    # (migrations, testing) and PoolDataSourceFactory.
    # May (-c)onfig via Iudex::Core::Config.connect_props=
    # Defaults are used by unit tests, migrate
    CONFIG = {
      :adapter  => 'jdbcpostgresql',
      :host     => 'localhost',
      :database => 'iudex_test',
      :username => 'iudex',
      :pool     => 10 }
  end

  module Core
    module Config
      # Merge props to Iudex::DA::CONFIG
      def self.connect_props=( props )
        Iudex::DA::CONFIG.merge!( props )
      end
    end
  end

end
