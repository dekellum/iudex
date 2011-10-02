require 'iudex-jetty-httpclient'

Iudex.configure do |c|

  c.setup_jetty_httpclient do
    { :timeout => 20_000,
      :max_connections_per_address => 2,
      :max_queue_size_per_address => 20 }
  end

end
