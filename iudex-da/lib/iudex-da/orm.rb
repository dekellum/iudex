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

      attr_reader :db

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
      # intpreted as the :target version.  Setup must be called
      # beforehand.
      def migrate( opts = {} )
        opts = {} if opts.nil?
        opts = { :target => opts } if opts.is_a?( Integer )
        raise "setup must be run before migrate" unless db
        profiles = Hooker.apply( [ :iudex, :migration_profiles ],
                                 opts[ :profiles ] || [] )

        pm = ProfileMigrator.new( db, profiles, opts )
        pm.run
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
