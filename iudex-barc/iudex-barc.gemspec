# -*- ruby -*- encoding: utf-8 -*-

gem 'rjack-tarpit', '~> 2.0.a'
require 'rjack-tarpit/spec'

$LOAD_PATH.unshift( File.join( File.dirname( __FILE__ ), 'lib' ) )

require 'iudex-barc/base'

RJack::TarPit.specify do |s|

  s.version  = Iudex::BARC::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'rjack-slf4j',           '~> 1.6.1'
  s.depend 'gravitext-util',        '~> 1.5.1'
  s.depend 'iudex-http',            '~> 1.1.0'

  s.depend 'minitest',              '~> 2.3',       :dev

  s.maven_strategy = :no_assembly

end
