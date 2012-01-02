# -*- ruby -*- encoding: utf-8 -*-

gem 'rjack-tarpit', '~> 2.0.a'
require 'rjack-tarpit/spec'

$LOAD_PATH.unshift( File.join( File.dirname( __FILE__ ), 'lib' ) )

require 'iudex-simhash/base'

RJack::TarPit.specify do |s|

  s.version  = Iudex::SimHash::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'iudex-core',            '~> 1.1.0'
  s.depend 'iudex-html',            '~> 1.1.0'

  s.depend 'minitest',              '~> 2.3',       :dev
  s.depend 'rjack-logback',         '~> 1.0',       :dev

  s.maven_strategy = :no_assembly

end
