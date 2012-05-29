# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'

spec = Gem::Specification.find_by_name("rjack-tarpit")
require spec.gem_dir + '/lib/rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-worker/base'

  s.version = Iudex::Worker::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',             '~> 1.2.b'
  s.depend 'iudex-da',               '~> 1.2.b'
  s.depend 'iudex-rome',             '~> 1.2.b'
  s.depend 'iudex-html',             '~> 1.2.b'
  s.depend 'iudex-simhash',          '~> 1.2.b'
  s.depend 'iudex-char-detector',    '~> 1.2.b'

  s.depend 'rjack-logback',          '~> 1.0'

  s.depend 'minitest',               '~> 2.3',       :dev
  s.depend 'iudex-httpclient-3',     '~> 1.2.b',     :dev
  s.depend 'iudex-jetty-httpclient', '~> 1.2.b',     :dev
  s.depend 'iudex-async-httpclient', '~> 1.2.b',     :dev

  s.platform = :java
end
