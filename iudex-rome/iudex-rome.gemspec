# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-rome/base'

  s.version = Iudex::ROME::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',            '~> 1.7'
  s.depend 'rjack-rome',            '~> 1.0.2'

  s.depend 'minitest',              '~> 4.7.4',     :dev
  s.depend 'rjack-logback',         '~> 1.5',       :dev

  s.maven_strategy = :no_assembly
  s.required_ruby_version = '>= 1.8.7'
  s.license = 'Apache-2.0'
end
