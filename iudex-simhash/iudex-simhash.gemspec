# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-simhash/base'

  s.version = Iudex::SimHash::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',            '~> 1.1.0'
  s.depend 'iudex-html',            '~> 1.1.0'

  s.depend 'minitest',              '~> 2.3',       :dev
  s.depend 'rjack-logback',         '~> 1.0',       :dev

  s.maven_strategy = :no_assembly
end
