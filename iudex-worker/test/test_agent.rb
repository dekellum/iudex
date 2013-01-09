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

require 'iudex-worker'

class TestAgent < MiniTest::Unit::TestCase
  include Iudex::Worker
  include RJack

  def setup
    Logback[ 'iudex.worker.FilterChainFactory' ].level = Logback::WARN
    Hooker.log_with { |m| SLF4J[ 'iudex' ].info( m.rstrip ) }
  end

  def teardown
    Logback[ 'iudex.worker.FilterChainFactory' ].level = nil
    Hooker.send( :clear )
  end

  def test_agent_default
    assert_agent
  end

  def test_agent_with_sample_config
    # Test out the sample config
    Hooker.load_file( File.join( File.dirname( __FILE__ ),
                                 '..', 'config', 'config.rb' ) )

    assert_agent
  end

  def test_agent_with_sample_config_jetty_http
    Hooker.load_file( File.join( File.dirname( __FILE__ ),
                                 '..', 'config', 'config_jetty_http.rb' ) )

    assert_agent
  ensure
    # Hack to avoid interference in test of other configs, given
    # require iudex-jetty-httpclient. This only works once, but
    # thats enough for testing.
    Iudex.send( :remove_const, :JettyHTTPClient )
  end

  def test_agent_with_sample_config_async_http
    Hooker.load_file( File.join( File.dirname( __FILE__ ),
                                 '..', 'config', 'config_async_http.rb' ) )

    assert_agent
  ensure
    # Same hack as above.
    Iudex.send( :remove_const, :AsyncHTTPClient )
  end

  def test_agent_graceful_shutdown_on_fcf_error
    Hooker.add( [ :iudex, :filter_factory ] ) do |fcf|
      def fcf.filters
        super
        raise "Test Badness in FCF"
      end
    end

    agent = Agent.new
    agent.run
    pass # returns
  end

  def assert_agent

    # Stub VisitManager.start/start_executor to allow agent.run to
    # return early.
    Hooker.add( [ :iudex, :visit_manager ] ) do |vm|
      def vm.start
        #disable
      end
      def vm.start_executor
        #disable
      end
    end

    agent = Agent.new
    agent.run
    pass

    Hooker.check_not_applied do |*args|
      flunk( "Hooks not applied: " + args.inspect )
    end
    pass

  end

end
