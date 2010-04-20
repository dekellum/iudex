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

require 'iudex-da'
require 'rjack-slf4j'
require 'java'
require 'jdbc/postgres'

module Iudex::DA

  # Factory for a DataSource using commons-dbcp and postgres driver
  class PoolDataSourceFactory
    import 'java.io.PrintWriter'
    import 'java.sql.DriverManager'
    import 'java.util.Properties'
    import 'java.util.regex.Pattern'
    import 'org.apache.commons.dbcp.DriverManagerConnectionFactory'
    import 'org.apache.commons.dbcp.PoolableConnectionFactory'
    import 'org.apache.commons.dbcp.PoolingDataSource'
    import 'org.apache.commons.pool.impl.GenericObjectPool'
    import 'iudex.util.LogWriter'

    attr_accessor :data_source

    def initialize( in_props = {} )
      @props = CONFIG.merge( in_props )

      # Tweeks specific for Java datasource/pool
      @props[ :user ] ||= @props[ :username ]
      @props.delete( :username )

      @props[ :loglevel ] ||= 1

      SLF4J[ 'iudex.da.PoolDataSourceFactory' ].info do
        "Init properties: #{@props.inspect}"
      end
      load_driver
    end

    def create
      con_factory = create_connection_factory
      @con_pool = create_connection_pool( con_factory )
      @data_source = PoolingDataSource.new( @con_pool )
    end

    def close
      @con_pool.close
      @con_pool = @data_source = nil
    end

    def load_driver
      import 'org.postgresql.Driver'
      lw = LogWriter.new( 'iudex.da.Driver' )
      # Remove postgres time stamp, trailing whitespace.
      lw.remove_pattern =
        Pattern.compile( '(^\d\d:\d\d:\d\d\.\d\d\d\s\(\d\)\s)|(\s+$)' )
      DriverManager::set_log_writer( PrintWriter.new( lw, true ) )
    end

    def create_connection_factory
      uri = "jdbc:postgresql://%s/%s" % [ @props[ :host ], @props[ :database ] ]

      jprops = Properties.new
      @props.each { |k,v| jprops.set_property( k.to_s, v.to_s ) }

      DriverManagerConnectionFactory.new( uri, jprops )
    end

    def create_connection_pool( con_factory )
      con_pool = GenericObjectPool.new( nil )

      con_count = @props[ :pool ]
      if con_count
        con_pool.max_active = con_count
        con_pool.max_idle = con_count
      end

      props = @props[ :ds_pool ]
      if props
        props.each { |k,v| con_pool.send( k.to_s + '=', v ) }
      end

      # This sets self on con_pool
      PoolableConnectionFactory.new( con_factory,
                                     con_pool,
                                     nil, #stmtPoolFactory
                                     nil, #validationQuery
                                     false, #read_only_default
                                     true ) #auto_commit_default
      con_pool
    end
  end

end
