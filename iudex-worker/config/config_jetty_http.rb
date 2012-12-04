require 'iudex-jetty-httpclient'

Iudex.configure do |c|

  c.setup_jetty_httpclient do
    { :connect_timeout => 12_000,
      :idle_timeout    => 20_000,
      :max_connections_per_address => 2,
      :max_queue_size_per_address => 20,
      :follow_redirects => false }
  end

end
