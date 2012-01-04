# -*- ruby -*- encoding: utf-8 -*-

gem 'rjack-tarpit', '~> 2.0.a'
require 'rjack-tarpit/spec'

$LOAD_PATH.unshift( File.join( File.dirname( __FILE__ ), 'lib' ) )

require 'iudex-brutefuzzy-protobuf/base'

RJack::TarPit.specify do |s|

  s.version  = Iudex::BruteFuzzy::Protobuf::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'rjack-protobuf',         '~> 2.4.1'
  s.depend 'minitest',               '~> 2.3',       :dev

  s.maven_strategy = :no_assembly

end
