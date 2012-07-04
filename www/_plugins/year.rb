module Jekyll
  class YearTag < Liquid::Tag

    def initialize( tag_name, path, tokens )
      super
      @path = path.strip
    end

    def render( context )
      Time.now.strftime( "%Y" )
    end
  end
end

Liquid::Template.register_tag( 'year', Jekyll::YearTag )
