# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

# Self contained spec for the "iudex" (uber) gem

RJack::TarPit.specify do |s|
  s.version = '1.4.0'

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-worker',             '~> 1.4.0'
  s.depend 'iudex-httpclient-3',       '~> 1.4.0'
  s.depend 'iudex-jetty-httpclient',   '~> 1.4.0'
  s.depend 'iudex-async-httpclient',   '~> 1.4.0'
  s.depend 'iudex-brutefuzzy-service', '~> 1.4.0'

  s.depend 'iudex-http-test',          '~> 1.4.0', :dev
  s.maven_strategy = :none
  s.platform = :java
end