# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'

spec = Gem::Specification.find_by_name("rjack-tarpit")
require spec.gem_dir + '/lib/rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-barc/base'

  s.version = Iudex::BARC::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'rjack-slf4j',           '~> 1.6.1'
  s.depend 'gravitext-util',        '~> 1.6.b'
  s.depend 'iudex-http',            '~> 1.2.b'

  s.depend 'minitest',              '~> 2.3',       :dev

  s.maven_strategy = :no_assembly
end
