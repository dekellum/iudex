# -*- ruby -*- encoding: utf-8 -*-

gem 'rjack-tarpit', '~> 2.0.a'
require 'rjack-tarpit/spec'

$LOAD_PATH.unshift( File.join( File.dirname( __FILE__ ), 'lib' ) )

require 'iudex-brutefuzzy-service/base'

RJack::TarPit.specify do |s|

  s.version  = Iudex::BruteFuzzy::Service::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',                '~> 1.1.0'
  s.depend 'rjack-logback',             '~> 1.0'
  s.depend 'iudex-simhash',             '~> 1.1.0'
  s.depend 'iudex-brutefuzzy-protobuf', '~> 1.0.0'
  s.depend 'rjack-jms',                 '~> 1.1.0'
  s.depend 'rjack-qpid-client',         '~> 0.12.0'

  s.depend 'minitest',                  '~> 2.3',       :dev

  s.maven_strategy = :no_assembly

end
