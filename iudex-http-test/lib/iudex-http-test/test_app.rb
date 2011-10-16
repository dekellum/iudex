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

require 'rack'

#FIXME: Otherwise rack can fail on threaded autoload (to be fixed in jruby)
require 'rack/mime'
require 'rack/head'

require 'sinatra/base'
require 'markaby'
require 'cgi'
require 'thread'
require 'rjack-slf4j'

require 'iudex-http-test/base'

module Iudex::HTTP::Test

  # Sets up rack.logger to write to rack.errors stream
  class SLogger
    include RJack
    def initialize( app, level = :info )
      @app = app
      @logger = SLF4J[ self.class ]
      @level = level

      def @logger.<<( msg )
        send( @level, msg )
      end
    end

    def call(env)
      env['rack.logger'] = @logger
      @app.call(env)
    end
  end

  class ConcurrentCounter
    def initialize
      @lock = Mutex.new
      @count = 0
    end

    def sync( &block )
      @lock.synchronize( &block )
    end

    def enter
      sync { @count += 1 }
    end

    def exit
      sync { @count -= 1 }
    end

    def count
      sync { @count }
    end
  end

  class TestApp < Sinatra::Base

    PUBLIC = File.expand_path( File.join( File.dirname( __FILE__ ),
                                          '..', '..', 'public' ) )

    set :environment,     :production
    set :show_exceptions, false
    set :raise_errors,    true
    set :dump_errors,     false
    use SLogger

    @@counter = ConcurrentCounter.new

    before do
      # Handle (con)currency param, halting if exceeded
      @con = params[:con].to_i # 0 if nil
      if @con > 0 && @@counter.enter > @con
        halt( 418, "Concurrency #{@con} exceeded" )
      end

      # Sleep now if requested
      s = params[:sleep].to_f # 0.0 if nil
      sleep s if s > 0.0
    end

    after do
      @@counter.exit if @con > 0
    end

    get '/' do
      redirect to( '/index' )
    end

    get '/301' do
      redirect( to( '/index' ), 301 ) #"redirect body"
    end

    get '/redirect' do
      loc = params[ :loc ]
      redirect loc if loc
      halt 400, "loc query parameter required"
    end

    get '/redirects/multi/:depth' do
      depth = params[:depth].to_i
      status = ( params[:code] || 302 ).to_i
      if depth > 1
        redirect( to( "/redirects/multi/#{depth - 1}#{common params}" ),
                  status )
      else
        "You finally made it"
      end
    end

    get '/index' do
      markaby do
        html do
          head { title "Test Index Page" }
          body do
            h1 "Iudex HTTP Test Service"
            a( :href => url( '/foobar' ) ) { "foobar" }
          end
        end
      end
    end

    get '/atom.xml' do
      send_file( "#{PUBLIC}/atom.xml",
                 :type => 'application/atom+xml' )
    end

    get '/env' do
      request.inspect
    end

    get '/error' do
      raise "Raising this ERROR for you"
    end

    get '/304' do
      halt 304
    end

    get '/concount' do
      content_type 'text/plain'
      @@counter.count.to_s
    end

    get '/echo/header/:name' do
      content_type 'text/plain'
      env[ 'HTTP_' + params[ :name ].gsub( /-/, '_' ).upcase ].to_s
    end

    class GiantGenerator
      FILLER = <<-END
        Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do
        eiusmod tempor incididunt ut labore et dolore magna aliqua.
      END

      def each
        loop { yield FILLER }
      end
    end

    get '/giant' do
      [ 200, { 'Content-Type' => 'text/plain' }, GiantGenerator.new ]
    end

    def common( params )
      ps = [ :sleep, :con ].
        map { |k| ( v = params[k] ) && [ k, v ] }.
        compact.
        map { |k,v| [ k, CGI.escape( v.to_s ) ].join( '=' ) }
      '?' + ps.join( '&' ) unless ps.empty?
    end

  end

end
