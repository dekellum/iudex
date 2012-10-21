# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-da/base'

  s.version = Iudex::DA::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',                          '~> 1.3.0'
  s.depend 'sequel',                              '~> 3.40.0'
  s.depend 'jdbc-postgres',                       '~> 9.1.901'
  s.depend 'rjack-commons-dbcp',                  '~> 1.4.0'
  s.depend 'rjack-commons-dbutils',               '~> 1.4.0'

  s.depend 'minitest',                            '~> 2.3',       :dev
  s.depend 'rjack-logback',                       '~> 1.2',       :dev

  s.maven_strategy = :no_assembly
end
