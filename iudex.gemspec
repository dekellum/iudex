# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

# Self contained spec for the "iudex" (uber) gem

RJack::TarPit.specify do |s|
  s.version = '1.5.0'

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-worker',             '>= 1.4.0', '< 1.7'
  s.depend 'iudex-httpclient-3',       '>= 1.4.0', '< 1.7'
  s.depend 'iudex-jetty-httpclient',   '>= 1.4.0', '< 1.7'
  s.depend 'iudex-async-httpclient',   '>= 1.4.0', '< 1.7'
  s.depend 'iudex-brutefuzzy-service', '>= 1.4.0', '< 1.7'

  s.depend 'iudex-http-test',          '>= 1.4.0', '< 1.7', :dev
  s.maven_strategy = :none
  s.platform = :java
  s.required_ruby_version = '>= 1.8.7'
  s.license = 'Apache-2.0'
end
