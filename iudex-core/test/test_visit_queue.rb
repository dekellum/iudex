#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2011 David Kellum
#
# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License.  You
# may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.  See the License for the specific language governing
# permissions and limitations under the License.
#++

require File.join( File.dirname( __FILE__ ), "setup" )
require 'iudex-core'

class TestVisitQueue < MiniTest::Unit::TestCase
  include Iudex::Core
  include Gravitext::HTMap

  UniMap.create_key( 'vtin' )

  UniMap.define_accessors

  def setup
    @visit_q = VisitQueue.new
    @visit_q.default_min_host_delay = 50 #ms
  end

  def test_priority_acquire
    check_priority
  end

  def test_priority_take
    check_priority( :take_order )
  end

  def check_priority( access_method = :acquire_order )
    orders = [ %w[ h a 1.3 ],
               %w[ h b 1.1 ],
               %w[ h c 1.2 ] ].map do |oinp|
      order( *oinp )
    end

    @visit_q.add_all( orders )

    orders.sort { |p,n| n.priority <=> p.priority }.each do |o|
      assert_equal( o.vtin, send( access_method ) )
    end

    assert_equal( 0, @visit_q.order_count )
  end

  def test_hosts_acquire
    check_priority_hosts
  end

  def test_hosts_take
    check_priority_hosts( :take_order )
  end

  def check_priority_hosts( access_method = :acquire_order )
    orders = [ %w[ h2 b 2.2 ],
               %w[ h2 a 2.1 ],
               %w[ h3 b 3.2 ],
               %w[ h3 a 3.1 ],
               %w[ h1 b 1.2 ],
               %w[ h1 a 1.1 ] ].map do |oinp|
      order( *oinp )
    end

    @visit_q.add_all( orders )

    expected_order = [ %w[ h3 b 3.2 ],
                       %w[ h2 b 2.2 ],
                       %w[ h1 b 1.2 ],
                       %w[ h3 a 3.1 ],
                       %w[ h2 a 2.1 ],
                       %w[ h1 a 1.1 ] ]

    expected_order.each do |o|
      assert_equal( o, send( access_method ) )
    end

    assert_equal( 0, @visit_q.order_count )
  end

  def acquire_order
    o = @visit_q.acquire( 100 )
    o.vtin
  ensure
    @visit_q.release( o.vtin.first, nil ) if o
  end

  def take_order
    hq = @visit_q.take( 100 )
    o = hq.remove
    o.vtin
  ensure
    @visit_q.untake( hq ) if hq
  end

  def order( host, c, p )
    UniMap.new.tap do |o|
      o.url = visit_url( "http://#{host}/#{c}" )
      o.priority = p.to_f
      o.vtin = [ host, c, p ]
    end
  end

  def visit_url( url )
    VisitURL.normalize( url )
  end

end
