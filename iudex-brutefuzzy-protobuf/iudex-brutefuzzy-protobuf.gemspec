# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-brutefuzzy-protobuf/base'

  s.version  = Iudex::BruteFuzzy::Protobuf::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'rjack-protobuf',         '~> 2.4.1'
  s.depend 'minitest',               '~> 2.3',       :dev

  s.maven_strategy = :no_assembly

  s.generated_files =
    [ 'src/main/java/iudex/brutefuzzy/protobuf/ProtocolBuffers.java' ]
end
