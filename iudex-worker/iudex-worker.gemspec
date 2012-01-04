# -*- ruby -*- encoding: utf-8 -*-

gem 'rjack-tarpit', '~> 2.0.a'
require 'rjack-tarpit/spec'

$LOAD_PATH.unshift( File.join( File.dirname( __FILE__ ), 'lib' ) )

require 'iudex-worker/base'

RJack::TarPit.specify do |s|

  s.version  = Iudex::Worker::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',             '~> 1.1.0'
  s.depend 'iudex-da',               '~> 1.1.0'
  s.depend 'iudex-rome',             '~> 1.1.0'
  s.depend 'iudex-html',             '~> 1.1.0'
  s.depend 'iudex-simhash',          '~> 1.1.0'
  s.depend 'iudex-char-detector',    '~> 1.1.0'

  s.depend 'rjack-logback',          '~> 1.0'

  s.depend 'minitest',               '~> 2.3',       :dev
  s.depend 'iudex-httpclient-3',     '~> 1.1.0',     :dev
  s.depend 'iudex-jetty-httpclient', '~> 1.1.0',     :dev
  s.depend 'iudex-async-httpclient', '~> 1.1.0',     :dev

  s.platform = :java

end
