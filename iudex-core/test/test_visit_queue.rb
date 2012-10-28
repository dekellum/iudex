#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2012 David Kellum
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

  import 'java.util.concurrent.Executors'
  import 'java.util.concurrent.TimeUnit'
  import 'java.lang.Runnable'

  UniMap.create_key( 'vtest_input' )

  UniMap.define_accessors

  def setup
    @visit_q = VisitQueue.new
    @visit_q.config( :delay => 50 ) #ms
    @scheduler = Executors::new_scheduled_thread_pool( 2 )
  end

  def teardown
    @scheduler.shutdown_now if @scheduler
  end

  def test_priority
    orders = [ %w[ h a 1.3 ],
               %w[ h b 1.1 ],
               %w[ h c 1.2 ] ].map do |oinp|
      order( oinp )
    end

    @visit_q.add_all( orders )

    orders.sort { |p,n| n.priority <=> p.priority }.each do |o|
      assert_equal( o.vtest_input, acquire_order )
    end

    assert_queue_empty
  end

  def add_common_orders
    [ %w[   h2 a 2.2 100 ],
      %w[ w.h2 b 2.1 ],
      %w[   h2 c 2.0 ],
      %w[   h3 a 3.2 130 ],
      %w[   h3 b 3.1 130 ],
      %w[ m.h3 c 3.0 ],
      %w[   h1 a 1.2 ],
      %w[   h1 b 1.1 ] ].each do |oinp|

      @visit_q.add( order( oinp ) )
    end

    assert_equal( 3, @visit_q.host_count, "host count" )
    assert_equal( 8, @visit_q.order_count, "order count" )
  end

  def test_hosts_acquire
    add_common_orders

    expected = [ %w[   h3 a 3.2 ],
                 %w[   h2 a 2.2 ],
                 %w[   h1 a 1.2 ],
                 %w[   h1 b 1.1 ],
                 %w[ w.h2 b 2.1 ],
                 %w[   h3 b 3.1 ],
                 %w[   h2 c 2.0 ],
                 %w[ m.h3 c 3.0 ] ]

    p = 0
    expected.each do |o|
      assert_equal( o, acquire_order, p += 1 )
    end

    assert_queue_empty
  end

  def test_configure
    @visit_q.config( :domain => 'h2.com', :delay => 75, :cons => 2 )

    [ %w[   h2 a 2.2 ],
      %w[ w.h2 b 2.1 ],
      %w[   h3 a 3.2 ],
      %w[   h3 b 3.1 ],
      %w[   h1 a 1.2 ],
      %w[   h1 b 1.1 ] ].each do |oinp|

      @visit_q.add( order( oinp ) )

    end
    assert_equal( 3, @visit_q.host_count, "host count" )

    expected = [ %w[   h3 a 3.2 ],
                 %w[   h2 a 2.2 ],
                 %w[   h1 a 1.2 ],
                 %w[   h3 b 3.1 ],
                 %w[   h1 b 1.1 ],
                 %w[ w.h2 b 2.1 ] ]

    p = 0
    expected.each do |o|
      assert_equal( o, acquire_order, p += 1 )
    end

    assert_queue_empty
  end

  def test_configure_type
    @visit_q.config( :domain => 'h2.com',
                     :delay => 75, :cons => 2 )
    @visit_q.config( :domain => 'h2.com', :type => 'ALT',
                     :delay => 50, :cons => 1 )

    [ %w[   h2     a 2.2 ],
      %w[ w.h2     b 2.1 ],
      %w[   h2:ALT c 3.2 ],
      %w[   h2:ALT d 3.1 ],
      %w[   h1     a 1.2 ],
      %w[   h1     b 1.1 ] ].each do |oinp|

      @visit_q.add( order( oinp ) )

    end
    assert_equal( 3, @visit_q.host_count, "host count" )

    expected = [ %w[   h2:ALT c 3.2 ],
                 %w[   h2     a 2.2 ],
                 %w[   h1     a 1.2 ],
                 %w[   h2:ALT d 3.1 ],
                 %w[   h1     b 1.1 ],
                 %w[ w.h2     b 2.1 ] ]

    p = 0
    expected.each do |o|
      assert_equal( o, acquire_order, p += 1 )
    end

    assert_queue_empty
  end

  def test_multi_access_2
    @visit_q.default_max_access_per_host = 2
    add_common_orders

    expected = [ %w[   h3 a 3.2 ],
                 %w[   h2 a 2.2 ],
                 %w[   h1 a 1.2 ],
                 %w[   h3 b 3.1 ],
                 %w[ w.h2 b 2.1 ],
                 %w[   h1 b 1.1 ],
                 %w[   h2 c 2.0 ],
                 %w[ m.h3 c 3.0 ] ]

    p = 0
    expected.each do |o|
      assert_equal( o, acquire_order, p += 1 )
    end

    assert_queue_empty
  end

  def test_multi_access_3
    @visit_q.default_max_access_per_host = 3
    add_common_orders

    expected = [ %w[   h3 a 3.2 ],
                 %w[   h2 a 2.2 ],
                 %w[   h1 a 1.2 ],
                 %w[   h3 b 3.1 ],
                 %w[ w.h2 b 2.1 ],
                 %w[   h1 b 1.1 ],
                 %w[ m.h3 c 3.0 ],
                 %w[   h2 c 2.0 ] ]

    p = 0
    expected.each do |o|
      assert_equal( o, acquire_order, p += 1 )
    end

    assert_queue_empty
  end

  def test_interleaved
    @visit_q.default_max_access_per_host = 2
    @visit_q.default_min_host_delay = 3 #ms
    @visit_q.config( :domain => 'h2.com', :delay => 1, :cons => 4 )

    512.times do |i|
      @visit_q.add( order( [ %w[ h1 h2 ][rand( 2 )], i, 5 * rand ] ) )
    end

    c = @visit_q.order_count
    added = 0

    while c > 0
      o = @visit_q.acquire( 300 )
      flunk( "acquire returned null" ) unless o
      c -= 1
      @scheduler.schedule( ReleaseJob.new( @visit_q, o ),
                           rand( 20_000 ), TimeUnit::MICROSECONDS )

      while ( added < 1024 ) && ( rand(3) != 1 )
        added += 1
        c += 1
        j = Job.new( added ) do | i, p |
          @visit_q.add( order( [ %w[ h1 h2 ][rand( 2 )], i, 5 * rand ] ) )
        end
        @scheduler.schedule( j, rand( 20_000 ), TimeUnit::MICROSECONDS )
      end

    end

    assert_queue_empty
  end

  def assert_queue_empty
    @scheduler.shutdown
    @scheduler.await_termination( 2, TimeUnit::SECONDS )
    @scheduler = nil
    assert_equal( 0, @visit_q.order_count, "order count" )
    assert_equal( 0, @visit_q.host_count,  "host count" )
  end

  def acquire_order
    o = @visit_q.acquire( 200 )
    if o
      o.vtest_input.tap do |i|
        delay = ( i[3] || 20 ).to_i
        @scheduler.schedule( ReleaseJob.new( @visit_q, o ),
                             delay,
                             TimeUnit::MILLISECONDS )
      end.slice( 0..2 )
    end
  end

  def order( args )
    host, c, p = args
    host, t = host.split( ':' )

    UniMap.new.tap do |o|
      o.url = visit_url( "http://#{host}.com/#{c}" )
      o.priority = p.to_f
      o.vtest_input = args
      o.type = t || 'PAGE'
    end
  end

  def visit_url( url )
    VisitURL.normalize( url )
  end

  LOG = RJack::SLF4J[ self ]

  class ReleaseJob
    include Runnable

    def initialize( visit_q, order )
      super()
      @visit_q = visit_q
      @order = order
    end

    def run
      @visit_q.release( @order, nil )
    rescue => e
      LOG.error( e )
    end
  end

  class Job
    include Runnable

    def initialize( *args, &block )
      @block = block
      @args = args
    end
    def run
      @block.call( *@args )
    rescue => e
      LOG.error( e )
    end
  end

end
