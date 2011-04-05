Iudex.configure do |c|

  c.setup_brutefuzzy_agent do |a|

    def a.create_fuzzy_set
      Iudex::SimHash::BruteFuzzy::FuzzyTree64.new( 8 * 1024 * 1024, 3, 16 )
    end

  end

  c.with( :jms ) do |jc|

    jc.setup_context do |ctx|
      # ctx.brokers = [ [ "host-a" ], [ "host-b" ] ]
    end

    jc.setup_connector do |cntr|
      # cntr.max_connect_delay = 60_000 #ms
    end

  end

end
