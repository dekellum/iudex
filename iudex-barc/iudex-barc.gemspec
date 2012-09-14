# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-barc/base'

  s.version = Iudex::BARC::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'rjack-slf4j',           '>= 1.6.5', '< 1.8'
  s.depend 'gravitext-util',        '~> 1.6.1'
  s.depend 'iudex-http',            '~> 1.2.1'

  s.depend 'minitest',              '~> 2.3',       :dev

  s.maven_strategy = :no_assembly
end
