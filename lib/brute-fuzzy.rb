
require 'brute-fuzzy/base.rb'

require 'java'
require 'gravitext-util'

module BruteFuzzy
  require "brute-fuzzy/brute-fuzzy-#{VERSION}.jar"
  import 'brutefuzzy.FuzzySet64'
  import 'brutefuzzy.FuzzySetPerfTest'
end
