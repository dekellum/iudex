# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex/base'

  s.version = Iudex::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-worker',             '~> 1.3.0'
  s.depend 'iudex-httpclient-3',       '>= 1.2.1', '< 1.4'
  s.depend 'iudex-jetty-httpclient',   '>= 1.2.1', '< 1.4'
  s.depend 'iudex-async-httpclient',   '>= 1.2.1', '< 1.4'
  s.depend 'iudex-brutefuzzy-service', '>= 1.2.1', '< 1.4'

  s.platform = :java
end
