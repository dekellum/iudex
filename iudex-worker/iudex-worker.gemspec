# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-worker/base'

  s.version = Iudex::Worker::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',             '>= 1.2.1', '< 1.4'
  s.depend 'iudex-da',               '~> 1.3.0'
  s.depend 'iudex-rome',             '>= 1.2.1', '< 1.4'
  s.depend 'iudex-html',             '>= 1.2.1', '< 1.4'
  s.depend 'iudex-simhash',          '>= 1.2.1', '< 1.4'
  s.depend 'iudex-char-detector',    '>= 1.2.1', '< 1.4'

  s.depend 'rjack-logback',          '~> 1.2'

  s.depend 'minitest',               '~> 2.3',       :dev
  s.depend 'iudex-httpclient-3',     '>= 1.2.1', '< 1.4', :dev
  s.depend 'iudex-jetty-httpclient', '>= 1.2.1', '< 1.4', :dev
  s.depend 'iudex-async-httpclient', '>= 1.2.1', '< 1.4', :dev

  s.platform = :java
end
