#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2015 David Kellum
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

class TestVisitManager < MiniTest::Unit::TestCase
  include Gravitext::HTMap
  include Iudex::Filter
  include Iudex::Filter::Core
  include Iudex::Core

  import 'iudex.core.GenericWorkPollStrategy'

  import 'java.util.concurrent.Executors'
  import 'java.util.concurrent.TimeUnit'
  import 'java.util.concurrent.CountDownLatch'

  UniMap.define_accessors

  def setup
    @latch = CountDownLatch.new( 20 )

    @manager = VisitManager.new( TestWorkPoller.new )
    @manager.do_wait_on_generation = false

    @scheduler = Executors::new_scheduled_thread_pool( 1 )

    test_filter = fltr do |order|
      @scheduler.schedule( proc { @manager.release( order, nil ) },
                           rand( 20_000 ), TimeUnit::MICROSECONDS )
      @latch.countDown();
    end

    @manager.filter_chain = FilterChain.new( "test", [ test_filter ] )

  end

  def teardown
    @scheduler.shutdown_now if @scheduler
    @manager.shutdown if @manager
  end

  def test
    @manager.start
    pass
    assert( @latch.await( 5, TimeUnit::SECONDS ) )
    @manager.shutdown
    pass
  end

  class TestWorkPoller < GenericWorkPollStrategy
    include Gravitext::HTMap
    include Iudex::Core

    def initialize
      super()
      self.min_poll_interval = 5
      self.max_check_interval = 21
      self.max_poll_interval = 130 #ms
      @batch = 0
      @log = RJack::SLF4J[ self.class ]
    end

    def log
      @log.java_logger
    end

    def pollWorkImpl( visit_q )
      @batch += 1

      [ %w[ h2 a 2.2 ],
        %w[ h2 b 2.1 ],
        %w[ h2 c 2.0 ],
        %w[ h3 a 3.2 ],
        %w[ h3 b 3.1 ],
        %w[ h3 c 3.0 ],
        %w[ h1 a 1.2 ],
        %w[ h1 b 1.1 ] ].each do |h,i,p|

        visit_q.add( order( h, @batch, i, p ) )
      end
    end

    def order( host, batch, i, p )
      UniMap.new.tap do |o|
        o.url = visit_url( "http://#{host}.com/#{batch}/#{i}" )
        o.priority = p.to_f
      end
    end

    def visit_url( url )
      VisitURL.normalize( url )
    end

  end

end
