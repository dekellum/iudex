# -*- ruby -*- encoding: utf-8 -*-

gem 'rjack-tarpit', '~> 2.0.a'
require 'rjack-tarpit/spec'

$LOAD_PATH.unshift( File.join( File.dirname( __FILE__ ), 'lib' ) )

require 'iudex-core/base'

RJack::TarPit.specify do |s|

  s.version  = Iudex::Core::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'rjack-slf4j',           '~> 1.6.1'
  s.depend 'hooker',                '~> 1.0.0'
  s.depend 'gravitext-util',        '~> 1.5.1'
  s.depend 'iudex-filter',          '~> 1.1.0'
  s.depend 'iudex-http',            '~> 1.1.0'
  s.depend 'iudex-barc',            '~> 1.1.0'

  s.depend 'minitest',              '~> 2.3',       :dev
  s.depend 'rjack-logback',         '~> 1.0',       :dev

  s.maven_strategy = :no_assembly

end
