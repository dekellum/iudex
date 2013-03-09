# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-char-detector/base'

  s.version = Iudex::CharDetector::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',            '~> 1.4.0'
  s.depend 'rjack-icu',             '~> 4.50.1'

  s.depend 'minitest',              '~> 4.6.2',     :dev
  s.depend 'rjack-logback',         '~> 1.5',       :dev

  s.maven_strategy = :no_assembly
end
