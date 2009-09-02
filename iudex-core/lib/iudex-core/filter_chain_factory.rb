
require 'iudex-core'

module Iudex
  module Filters
    import 'iudex.filters.FilterChain'
    import 'iudex.filters.ListenerChain'
    import 'iudex.filters.FilterIndex'
    import 'iudex.filters.LogListener'
    import 'iudex.filters.SummaryReporter'

    class FilterChainFactory
      attr_accessor :filters, :listeners

      def initialize( description )
        @filters = []

        @log = SLF4J::Logger.new( self.class )

        @index = FilterIndex.new
        ll = LogListener.new( [ FilterChain.name, description ].join( '.' ),
                              @index )

        @listeners = [ ll ]

        @description = description
      end

      def create
        @chain = FilterChain.new( @description, @filters )
        @listener = ListenerChain.new( @listeners )
        @chain.listener = @listener

        @chain, @listener
      end

      def close
        @chain.close unless @chain.nil?
        @chain = nil

        @listener.close unless @listener.nil?
        @listener = nil
      end

      def do_filter
        chain, listener = create
        yield chain
      ensure
        close
      end

    end

  end
end
