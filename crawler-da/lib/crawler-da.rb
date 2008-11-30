
require 'crawler-da/base'
require 'jdbc/postgres'

module CrawlerDA
  Dir.glob( File.join( CRAWLER_DA_DIR, '*.jar' ) ).each { |jar| require jar }

  # Factory for a DataSource using commons-dbcp and postgres driver
  class PoolDataSourceFactory
    import 'java.util.Properties'
    import 'org.apache.commons.dbcp.DriverManagerConnectionFactory'
    import 'org.apache.commons.dbcp.PoolableConnectionFactory'
    import 'org.apache.commons.dbcp.PoolingDataSource'
    import 'org.apache.commons.pool.impl.GenericObjectPool'

    attr_accessor :data_source 

    def initialize( properties )
      @properties = properties.dup
      @properties[ 'user' ] ||= @properties[ 'username' ]
      @properties[ 'host' ] ||= 'localhost'
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
    end

    def create_connection_factory
      uri = "jdbc:postgresql://#{ @properties[ 'host' ] }/" + 
        "#{ @properties[ 'database' ] }"

      props = Properties.new
      @properties.each do | key, value | 
        props.set_property( key.to_s, value.to_s )
      end
      
      DriverManagerConnectionFactory.new( uri, props )
    end

    def create_connection_pool( con_factory )
      con_pool = GenericObjectPool.new( nil )
      # FIXME: Has min, max connections, etc. setters
      # Defaults to 8 max

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
