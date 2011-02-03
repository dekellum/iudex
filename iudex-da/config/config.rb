
Iudex.configure do |c|

  # Set DA connection properties
  c.setup( :connect_props ) do |p|
    { :host     => 'localhost',
      :database => 'iudex_test',
      :username => 'iudex',
      :ds_pool     => { :max_active => 4,
                        :max_idle   => 2 },
      :loglevel => 2 }
  end

end
