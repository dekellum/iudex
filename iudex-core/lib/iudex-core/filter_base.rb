require 'iudex-core'

module Iudex
  module Filters

    import 'iudex.core.Filter'
    import 'iudex.core.Described'
    import 'iudex.core.Named'

    class FilterBase
      include Filter
      include Described
      include Named

      def describe
        []
      end

      def name
        n = self.class.name
        n = n.gsub( /::/, '.' )
        n = n.gsub( /(\w)\w+\./ ) { |m| $1.downcase + '.' }
        n
      end

      def filter( map )
        true
      end
    end

  end
end
