# Add support for liquid variable:
#
#   {{ page.root }} : relative path to site root
#

module Jekyll

  class Page

    # Calculate relative path from here to root of site
    def root
      path = File.dirname( File.join( @dir, self.url ) )
      c = 0;
      path.scan( %r{/[^/]+} ) { c += 1 };
      ( c == 0 ) ? '.' : ( ['..'] * c ).join( '/' )
    end

    def to_liquid
      # Add root as value
      self.data.deep_merge({
        "url"        => File.join(@dir, self.url),
        "content"    => self.content,
        "root"       => self.root }) #New root
    end

  end
end

# FIXME: Also need to add this to Post?
