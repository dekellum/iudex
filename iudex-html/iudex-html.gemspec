# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-html/base'

  s.version = Iudex::HTML::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',            '~> 1.3.0'
  s.depend 'rjack-nekohtml',        '~> 1.9.14'
  s.depend 'gravitext-xmlprod',     '~> 1.7.0'

  s.depend 'minitest',              '~> 2.3',       :dev
  s.depend 'rjack-logback',         '~> 1.5',       :dev

  s.maven_strategy = :no_assembly

end
