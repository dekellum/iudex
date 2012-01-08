# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-http-test/base'

  s.version  = Iudex::HTTP::Test::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'fishwife',              '~> 1.1.0'
  s.depend 'sinatra',               '~> 1.3.1'
  s.depend 'builder',               '~> 2.1.2' #constrain markaby
  s.depend 'markaby',               '~> 0.7.1'
  s.depend 'minitest',              '~> 2.3'

  s.depend 'rack-test',             '~> 0.6.0',       :dev
  s.depend 'rjack-logback',         '~> 1.0',         :dev

  s.platform = :java
end
