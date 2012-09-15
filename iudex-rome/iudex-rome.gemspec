# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-rome/base'

  s.version = Iudex::ROME::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',            '~> 1.2.1'
  s.depend 'rjack-rome',            '~> 1.0.2'

  s.depend 'minitest',              '~> 2.3',       :dev
  s.depend 'rjack-logback',         '~> 1.2',       :dev

  s.maven_strategy = :no_assembly

end
