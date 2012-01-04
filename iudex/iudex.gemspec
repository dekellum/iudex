# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0.a'
require 'rjack-tarpit/spec'

$LOAD_PATH.unshift( File.join( File.dirname( __FILE__ ), 'lib' ) )

require 'iudex/base'

RJack::TarPit.specify do |s|

  s.version  = Iudex::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-worker',           '~> 1.1.0'
  s.depend 'iudex-httpclient-3',     '~> 1.1.0'
  s.depend 'iudex-jetty-httpclient', '~> 1.1.0'
  s.depend 'iudex-async-httpclient', '~> 1.1.0'

  s.platform = :java
end
