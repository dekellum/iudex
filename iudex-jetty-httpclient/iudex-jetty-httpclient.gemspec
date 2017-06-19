# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-jetty-httpclient/base'

  s.version = Iudex::JettyHTTPClient::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-http',            '~> 1.7'
  s.depend 'rjack-jetty',           '>= 9.2.7', '< 9.5'
  s.depend 'hooker',                '~> 1.0.0'

  s.depend 'minitest',              '~> 4.7.4',     :dev
  s.depend 'iudex-http-test',       '~> 1.7',       :dev
  s.depend 'rjack-logback',         '~> 1.5',       :dev

  s.maven_strategy = :no_assembly
  s.required_ruby_version = '>= 1.8.7'
  s.license = 'Apache-2.0'
end
