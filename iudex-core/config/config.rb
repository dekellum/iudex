
Iudex.configure do |c|

  c.not_specified { raise "Shouldn't call" }

  c.setup_visit_executor do |vx|
    vx.max_threads             = 10
    vx.min_host_delay          = 2_000
    vx.max_shutdown_wait       = 19_000
  end

end
