# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-core/base'

  s.version = Iudex::Core::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'rjack-slf4j',           '~> 1.6.1'
  s.depend 'hooker',                '~> 1.0.0'
  s.depend 'gravitext-util',        '~> 1.6.b'
  s.depend 'iudex-filter',          '~> 1.2.b'
  s.depend 'iudex-http',            '~> 1.2.b'
  s.depend 'iudex-barc',            '~> 1.2.b'

  s.depend 'minitest',              '~> 2.3',       :dev
  s.depend 'rjack-logback',         '~> 1.0',       :dev

  s.maven_strategy = :no_assembly
end
