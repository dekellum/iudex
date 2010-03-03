#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

require 'brute-fuzzy'
require 'test/unit'

class TestFuzzySet < Test::Unit::TestCase
  def setup
    @fset = BruteFuzzy::FuzzySet64.new( 100, 4 )
  end

  def test_fuzzy_match
    assert( @fset.fuzzy_match( 0, 0 ) )

    assert( @fset.fuzzy_match( 0x7FFF_FFFF_FFFF_FFFF,
                               0x7FFF_FFFF_7777_FFFF ) )
    assert( ! @fset.fuzzy_match( 0x7FFF_FFFF_FFFF_FFFF,
                                 0x7FFF_FFFF_EFFF_7777 ) )
  end

  def test_add
    assert(   @fset.add(  0x0 ) )
    assert(   @fset.add( 0xFF ) )
    assert( ! @fset.add( 0xFE ) )
    assert( ! @fset.add(  0x1 ) )
  end
end
