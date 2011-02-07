Iudex.configure do |c|

  c.setup_brutefuzzy_agent do |a|
    def a.create_fuzzy_set
      Iudex::SimHash::BruteFuzzy::FuzzyTree64.new( 8 * 1024 * 1024, 3, 16 )
    end
  end

end
