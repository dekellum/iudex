require 'iudex-async-httpclient'

Iudex.configure do |c|

  c.setup_async_httpclient do
    { :connection_timeout_in_ms      =>  5_000,
      :request_timeout_in_ms         => 10_000,
      :idle_connection_timeout_in_ms =>  6_000,
      :maximum_connections_total     =>    200,
      :maximum_connections_per_host  =>      5 }
  end

end
