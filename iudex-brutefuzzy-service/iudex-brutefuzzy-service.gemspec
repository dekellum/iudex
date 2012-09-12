# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-brutefuzzy-service/base'

  s.version = Iudex::BruteFuzzy::Service::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',                '~> 1.2.1'
  s.depend 'rjack-logback',             '~> 1.0'
  s.depend 'iudex-simhash',             '~> 1.2.1'
  s.depend 'iudex-brutefuzzy-protobuf', '~> 1.2.1'
  s.depend 'rjack-jms',                 '~> 1.1.0'
  s.depend 'rjack-qpid-client',         '>= 0.14', '< 0.19'

  s.depend 'minitest',                  '~> 2.3',       :dev

  s.maven_strategy = :no_assembly
end
