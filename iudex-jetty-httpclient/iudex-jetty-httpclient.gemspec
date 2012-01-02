# -*- ruby -*- encoding: utf-8 -*-

gem 'rjack-tarpit', '~> 2.0.a'
require 'rjack-tarpit/spec'

$LOAD_PATH.unshift( File.join( File.dirname( __FILE__ ), 'lib' ) )

require 'iudex-jetty-httpclient/base'

RJack::TarPit.specify do |s|

  s.version  = Iudex::JettyHTTPClient::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-http',            '~> 1.1.0'
  s.depend 'rjack-jetty',           '~> 7.5.2'
  s.depend 'hooker',                '~> 1.0.0'

  s.depend 'minitest',              '~> 2.3',       :dev
  s.depend 'iudex-http-test',       '~> 1.1.0',     :dev
  s.depend 'rjack-logback',         '~> 1.0',       :dev

  s.maven_strategy = :no_assembly

end
