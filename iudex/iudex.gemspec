# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'

spec = Gem::Specification.find_by_name("rjack-tarpit")
require spec.gem_dir + '/lib/rjack-tarpit/spec'
RJack::TarPit.specify do |s|
  require 'iudex/base'

  s.version = Iudex::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-worker',           '~> 1.2.b'
  s.depend 'iudex-httpclient-3',     '~> 1.2.b'
  s.depend 'iudex-jetty-httpclient', '~> 1.2.b'
  s.depend 'iudex-async-httpclient', '~> 1.2.b'
  s.depend 'iudex-brutefuzzy-service', '~> 1.2.b'

  s.platform = :java
end
