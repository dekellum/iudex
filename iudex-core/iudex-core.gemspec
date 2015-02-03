# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-core/base'

  s.version = Iudex::Core::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'rjack-slf4j',           '~> 1.7.0'
  s.depend 'hooker',                '~> 1.0.0'
  s.depend 'gravitext-util',        '~> 1.7.0'
  s.depend 'iudex-filter',          '~> 1.7'
  s.depend 'iudex-http',            '~> 1.7'
  s.depend 'iudex-barc',            '~> 1.7'

  s.depend 'minitest',              '~> 4.7.4',     :dev
  s.depend 'rjack-logback',         '~> 1.5',       :dev

  s.maven_strategy = :no_assembly
  s.required_ruby_version = '>= 1.8.7'
  s.license = 'Apache-2.0'
end
