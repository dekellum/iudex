
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

end
