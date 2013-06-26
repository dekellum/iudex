require 'iudex-jetty-httpclient'

Iudex.configure do |c|

  c.setup_jetty_httpclient do
    { :timeout         => 35_000,
      :connect_timeout => 12_000,
      :idle_timeout    => 20_000,
      :max_connections_per_destination => 2,
      :max_requests_queued_per_destination => 20,
      :follow_redirects => false }
  end

end
