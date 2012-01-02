# -*- ruby -*- encoding: utf-8 -*-

gem 'rjack-tarpit', '~> 2.0.a'
require 'rjack-tarpit/spec'

$LOAD_PATH.unshift( File.join( File.dirname( __FILE__ ), 'lib' ) )

require 'iudex-da/base'

RJack::TarPit.specify do |s|

  s.version  = Iudex::DA::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',                          '~> 1.1.0'
  s.depend 'activerecord',                        '~> 3.0.10'
  s.depend 'jdbc-postgres',                       '~> 9.0.801'
  s.depend 'activerecord-jdbcpostgresql-adapter', '~> 1.1.3'
  s.depend 'rjack-commons-dbcp',                  '~> 1.4.0'
  s.depend 'rjack-commons-dbutils',               '~> 1.3.0'

  s.depend 'minitest',                            '~> 2.3',       :dev
  s.depend 'rjack-logback',                       '~> 1.0',       :dev

  s.maven_strategy = :no_assembly

end
