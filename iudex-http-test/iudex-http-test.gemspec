# -*- ruby -*- encoding: utf-8 -*-

gem 'rjack-tarpit', '~> 2.0.a'
require 'rjack-tarpit/spec'

$LOAD_PATH.unshift( File.join( File.dirname( __FILE__ ), 'lib' ) )

require 'iudex-http-test/base'

RJack::TarPit.specify do |s|

  s.version  = Iudex::HTTP::Test::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'fishwife',              '~> 1.1.0'
  s.depend 'sinatra',               '~> 1.3.1'
  s.depend 'markaby',               '~> 0.7.1'
  s.depend 'minitest',              '~> 2.3'

  s.depend 'rack-test',             '~> 0.6.0',       :dev

  s.platform = :java
end
