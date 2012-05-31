
Iudex.configure do |iudex|

  # Number of threads. A variable used for the configuration
  # below. Since the configuration itself is Ruby you can do
  # as much as you like. For example JRuby provides the number
  # of cores using java.lang.Runtime.getRuntime.availableProcessors
  threads = 3

  iudex.setup_connect_props do
    {
      # Your database information
      :database => 'crawl',
      :username => 'crawler',
      :password => 'sic-semper-tyrannosaurus-rex',

      # Configurable but defaults are included
      :host => 'localhost',
      :port => 5432,
    
      # Database connection pool configuration
      :ds_pool => {
        :max_active => (threads/3*2),
        :max_idle   => (threads/3)
      },

      # Logging configuration
      :loglevel => 1
    }
  end

  # Set up some of the information on the HTTP Client that Iudex
  # will use. There are multiple HTTP client implementations but
  # the default is the Apache HTTPClient3 so we'll just configure that.
  iudex.setup_http_client_3 do |mgr|
    mgr.manager_params.max_total_connections = (threads * 10)
  end

  # Visit manager manages most of the Iudex work. There are many options that will be
  # be documented in a seperate advanced configuration guide.
  iudex.setup_visit_manager do |visit_mgr|
    # Number of iudex threads. Default is 10.
    visit_mgr.max_threads = threads
  end

  # Configure the Iudex visit queue, which is resposible for politeness.
  iudex.setup_visit_queue do |queue|
    # Minimum time between fetches to a single host (in milliseconds)
    queue.default_min_host_delay = 100 

    # Concurrent requests per host
    queue.default_max_access_per_host = 1

    # You can also override the defaults above for a single host. For example
    # this allows a single host to have a 100ms minimum delay and 2 concurrent requests
    queue.configure_host("iudex.gravitext.com", 100, 2)
  end

  # The Iudex filter factory is responsible for configuring what actions take place for
  # each event during a crawl. There are many options but some of the most
  # common are included below.
  iudex.setup_filter_factory do |filter_factory|

    # The page_post methods is called wherever there is a new or updated page
    # to process.
    def filter_factory.page_post
      # Get the default filter chain
      default_filter_chain = super

      # Create out own filter. For anything besides an example this would be a class
      # that inherits from Iudex::Filter::FilterBase but for the example I'll use an anonymous class:
      my_filter = Iudex::Filter::FilterBase.new
      class << my_filter
        def filter(content)
      	  # The content argument is a Java UniMap that contains all of the information
      	  # about the new page, including the previous state. For an example let's print
      	  # out the URL being processed:
      	  puts "Processed Url: #{content.get(:url)}"
      	end
      end

      # Append out filter to the chain
      default_filter_chain << my_filter

      # Helpful message during startup
      puts "-------> New filter added"

      return default_filter_chain
    end
    
    # Customize the BARC file writer to turn off compression.
    def filter_factory.barc_writer
      # Use the Ruby 1.9 style tap method for chaining. More concise than the super method above.
      super.tap do |writer|
        writer.do_compress = false
      end
    end

    # Customize the BARC directory to set the file size for output
    def filter_factory.barc_directory
      super.tap do |barc_dir|
        barc_dir.target_length = 2 * ( 1024 ** 2 )
      end
    end

  end

end
