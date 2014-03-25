# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-html/base'

  s.version = Iudex::HTML::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',            '~> 1.4.0'
  s.depend 'rjack-nekohtml',        '~> 1.9.18'
  s.depend 'gravitext-xmlprod',     '~> 1.7.0'

  s.depend 'minitest',              '~> 4.7.4',     :dev
  s.depend 'rjack-logback',         '~> 1.5',       :dev

  s.maven_strategy = :no_assembly
  s.required_ruby_version = '>= 1.8.7'
  s.license = 'Apache-2.0'
end
