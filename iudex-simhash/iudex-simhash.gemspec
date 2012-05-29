# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'

spec = Gem::Specification.find_by_name("rjack-tarpit")
require spec.gem_dir + '/lib/rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-simhash/base'

  s.version = Iudex::SimHash::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',            '~> 1.2.b'
  s.depend 'iudex-html',            '~> 1.2.b'

  s.depend 'minitest',              '~> 2.3',       :dev
  s.depend 'rjack-logback',         '~> 1.0',       :dev

  s.maven_strategy = :no_assembly
end
