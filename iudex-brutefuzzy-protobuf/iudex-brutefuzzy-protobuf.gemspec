# -*- ruby -*-

gem 'rjack-tarpit', '~> 2.0'
require 'rjack-tarpit/spec'

RJack::TarPit.specify do |s|
  require 'iudex-brutefuzzy-protobuf/base'

  s.version = Iudex::BruteFuzzy::Protobuf::VERSION

  s.add_developer( 'David Kellum', 'dek-oss@gravitext.com' )

  s.depend 'rjack-protobuf',         '~> 2.5.0'
  s.depend 'minitest',               '~> 4.7.4',     :dev

  s.maven_strategy = :no_assembly

  s.generated_files =
    [ 'src/main/java/iudex/brutefuzzy/protobuf/ProtocolBuffers.java' ]
end
