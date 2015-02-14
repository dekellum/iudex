# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-da/base'

  s.version = Iudex::DA::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',                          '~> 1.7'
  s.depend 'sequel',                              '~> 4.8'
  s.depend 'rjack-jdbc-postgres',                 '~> 9.3'
  s.depend 'rjack-commons-dbcp',                  '~> 1.4.0'
  s.depend 'rjack-commons-dbutils',               '~> 1.5.0'

  s.depend 'minitest',                            '~> 4.7.4',     :dev
  s.depend 'rjack-logback',                       '~> 1.5',       :dev

  s.maven_strategy = :no_assembly
  s.required_ruby_version = '>= 1.8.7'
  s.license = 'Apache-2.0'
end
