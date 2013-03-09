# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-httpclient-3/base'

  s.version = Iudex::HTTPClient3::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-http',            '~> 1.4.0'
  s.depend 'rjack-httpclient-3',    '~> 3.1.3'
  s.depend 'hooker',                '~> 1.0.0'

  s.depend 'minitest',              '~> 4.6.2',     :dev
  s.depend 'iudex-http-test',       '~> 1.4.0',     :dev
  s.depend 'rjack-logback',         '~> 1.5',       :dev

  s.maven_strategy = :no_assembly
end
