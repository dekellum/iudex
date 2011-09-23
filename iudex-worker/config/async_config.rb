
RJack::Logback[ 'iudex.filter.core.FilterChain.agent' ].level =
  RJack::Logback::DEBUG

Iudex.configure do |c|

  threads = 5

  c.setup_connect_props do
    { :database => 'iudex_test',
      :ds_pool  => { :max_active => threads / 3 * 2,
                     :max_idle   => threads / 3 },
      :loglevel => 1 }
  end

  c.setup_worker do |worker|
    worker.run_async = true
  end

  c.setup_jetty_httpclient do
    { :timeout => 20_000,
      :max_connections_per_address => 2,
      :max_queue_size_per_address => 20 }
  end

  c.setup_visit_manager do |vx|
    vx.max_threads = threads
  end

  c.setup_visit_queue do |q|
    q.default_min_host_delay = 100 #ms
    q.default_max_access_per_host = 1

    q.configure_host( "gravitext.com", 100, 2 ) # 100ms, 2 connections
  end

  c.setup_work_poller do |wp|
    wp.min_order_remaining_ratio = 0.30
    wp.max_check_interval = 100 #ms
    wp.min_poll_interval = 2_000 #ms
  end

  c.setup_filter_factory do |ff|

    def ff.barc_writer
      super.tap do |w|
        w.do_compress = false
      end
    end

    def ff.barc_directory
      super.tap do |bdir|
        bdir.target_length = 2 * ( 1024 ** 2 )
      end
    end

  end

end
