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
require 'sequel'
require 'jdbc/postgres'
require 'hooker'

Sequel.extension :migration

module Iudex::DA

  module ORM

    class << self

      # The Sequel::Database instance. #setup is called if necessary.
      def db
        setup unless @db
        @db
      end

      # Setup the ORM (Sequel) connection given CONFIG defaults, any
      # passed opts, and connect_props config hooks.
      def setup( opts = {} )

        @db.disconnect if @db

        log = RJack::SLF4J[ "iudex.da.sequel" ]
        conf = CONFIG.merge( opts )
        conf = Hooker.merge( [ :iudex, :connect_props ], conf )

        conf[ :loggers ] = [ log ] if conf[ :log ]

        cstr = ( "%s://%s/%s?%s" %
                 [ conf[ :adapter ],
                   [ conf[ :host ], conf[ :port ] ].compact.join( ':' ),
                   conf[ :database ],
                   params( conf ) ] )

        log.info { "Connecting: #{cstr}" }
        log.debug { "Full Params: #{ conf.inspect }" }

        @db = Sequel.connect( cstr, conf )

      end

      # Migrate the DB given opts, including :target version.  For
      # backward compatibility, opts may be a single Integer,
      # interpreted as the :target version.  Setup must be called
      # beforehand.
      # See also opts for #migrate_ar_to_sequel
      def migrate( opts = {} )
        opts = {} if opts.nil?
        opts = { :target => opts } if opts.is_a?( Integer )
        raise "setup must be run before migrate" unless db
        profiles = Hooker.apply( [ :iudex, :migration_profiles ],
                                 opts[ :profiles ] || [] )

        migrate_ar_to_sequel( opts )

        pm = ProfileMigrator.new( db, profiles, opts )
        pm.run
      end

      # Migrate from a iudex [1.1.0,1.3) database managed by
      # activerecord to a 1.3.x database managed by Sequel. No-op if
      # already Sequel.
      # === Options
      # :ar_to_sequel_migrations:: Hash<Integer,String> AR migration
      #                            number to sequel filename
      #                            (timestamp) map for extensions
      #                            supported externally to iudex-da.
      def migrate_ar_to_sequel( opts )

        columns = ( db.table_exists?( :schema_migrations ) &&
                    db.schema( :schema_migrations ).map { |sr| sr[0] } )

        if columns == [ :version ] # Old format AR schema_migrations
          db.transaction do
            versions = db.from( :schema_migrations ).
              map { |r| r[ :version ].to_i }

            if ( versions & AR_REQUIRED ) != AR_REQUIRED
              missing = AR_REQUIRED - ( versions & AR_REQUIRED )
              raise( ARNotComplete,
                     "Missing AR migrations #{missing.inspect}; " +
                     "Use 'iudex-migrate _1.2.1_' first" )
            end

            migrations_map = AR_TO_SEQUEL_MIGRATIONS.
              merge( opts[ :ar_to_sequel_migrations ] || {} )

            db.drop_table( :schema_migrations )
            db.create_table( :schema_migrations ) do
              String :filename, :null => false
              primary_key [ :filename ]
            end

            sm = db[:schema_migrations]
            sm.insert( :filename => '20111012173757_base.rb' )

            migrations_map.each do | version, filename |
              sm.insert( :filename => filename ) if versions.include?( version )
            end
          end
        end
      end

      def params( opts )
        pms = {}

        u = opts[ :username ]
        pms[ :user ] = u if u

        p = opts[ :password ]
        pms[ :password ] = p if p

        pms.sort.map { |*p| p.join( '=' ) }.join( '&' )
      end

    end

    AR_TO_SEQUEL_MIGRATIONS = {
       85 => '21500000000001_add_simhash_index.rb',
      100 => '21500000000101_add_index_next_visit.rb'
    }
    AR_REQUIRED = [ 10, 20, 21, 30, 40, 50, 60, 70, 80, 81, 110 ]

    ARNotComplete = Class.new(StandardError)

    @db = nil

    # Custom migrator handling "profile" directories (optional
    # migrations)
    class ProfileMigrator < Sequel::TimestampMigrator

      def initialize( db, profiles, opts )

        base = File.join( LIB_DIR, '..', '..', 'db' )
        paths = [ base ]

        paths += profiles.compact.map do |p|
          p = p.to_s
          if p =~ %r{^/}
            p
          else
            File.join( base, p )
          end
        end

        @pattern = if paths.size > 1
                     '{' + paths.join( ',' ) + '}'
                   else
                     paths.first
                   end

        super( db, base, opts )
      end

      def get_migration_files
        Dir.glob( "#{@pattern}/[0-9]*_*.rb" )
      end

    end

  end

end
