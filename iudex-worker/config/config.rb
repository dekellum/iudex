
require 'iudex-da'
require 'iudex-httpclient-3'

module RJack::Logback
  logger( 'iudex.filter.core.FilterChain.agent' ).level = DEBUG
end

Iudex.configure do |c|

  concurrency = 3

  c.connect_props = {
    :ds_pool  => { :max_active => concurrency,
                   :max_idle   => concurrency * 2 / 3 },
    :loglevel => 1
  }

  c.setup_http_client_3 do |mgr|
    mgr.manager_params.max_total_connections = 100
  end

  c.setup_visit_executor do |vx|
    vx.max_threads = concurrency
    vx.min_host_delay = 20
  end

  c.setup_filter_factory do |ff|

    def ff.barc_writer
      bw = super
      bw.do_compress = false
      bw
    end

    def ff.barc_directory
      bdir = super
      bdir.setTargetBARCLength( 64 * 1024 )
      bdir
    end

  end

end
