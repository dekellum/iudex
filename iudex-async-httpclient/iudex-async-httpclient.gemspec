# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-async-httpclient/base'

  s.version = Iudex::AsyncHTTPClient::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-http',             '~> 1.4.0'
  s.depend 'rjack-async-httpclient', '~> 1.7.11'
  s.depend 'hooker',                 '~> 1.0.0'

  s.depend 'minitest',              '~> 4.7.4',     :dev
  s.depend 'iudex-http-test',       '~> 1.4.0',     :dev
  s.depend 'rjack-logback',         '~> 1.5',       :dev

  s.maven_strategy = :no_assembly
  s.required_ruby_version = '>= 1.8.7'
  s.license = 'Apache-2.0'
end
