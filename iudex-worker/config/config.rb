
require 'iudex-da'
require 'iudex-httpclient-3'

Iudex.configure do |c|

  # Set DA connection properties
  c.connect_props = {
    :host     => 'localhost',
    :database => 'iudex_test',
    :username => 'iudex',
    :ds_pool     => { :max_active => 4,
                      :max_idle   => 2 },
    :loglevel => 2 }

  c.setup_http_client_3 do |mgr|
    mgr.manager_params.max_total_connections = 200
  end

  c.setup_visit_executor do |vx|
    vx.max_threads             = 10
    vx.min_host_delay          = 2_000
    vx.max_shutdown_wait       = 19_000
  end

end
