# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-jetty-httpclient/base'

  s.version = Iudex::JettyHTTPClient::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-http',            '~> 1.2.1'
  s.depend 'rjack-jetty',           '>= 7.5.4', '< 7.7'
  s.depend 'hooker',                '~> 1.0.0'

  s.depend 'minitest',              '~> 2.3',       :dev
  s.depend 'iudex-http-test',       '~> 1.2.1',     :dev
  s.depend 'rjack-logback',         '~> 1.2',       :dev

  s.maven_strategy = :no_assembly
end
