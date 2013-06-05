# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-http-test/base'

  s.version = Iudex::HTTP::Test::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'fishwife',              '~> 1.5.0'
  s.depend 'sinatra',               '~> 1.4.2'
  s.depend 'builder',               '~> 3.2.0' #constrain markaby
  s.depend 'markaby',               '~> 0.7.2'
  s.depend 'minitest',              '~> 4.7.4'

  s.depend 'rack-test',             '~> 0.6.2',       :dev
  s.depend 'rjack-logback',         '~> 1.5',         :dev

  s.platform = :java
end
