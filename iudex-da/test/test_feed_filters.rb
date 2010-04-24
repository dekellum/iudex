#!/usr/bin/env jruby
#.hashdot.profile += jruby-shortlived

#--
# Copyright (c) 2008-2010 David Kellum
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

Logback.config_console( :stderr => true, :mdc => "uhash" )

require 'iudex-da'
require 'iudex-da/pool_data_source_factory'

require 'iudex-core/filter_chain_factory'
require 'iudex-httpclient-3' #FIXME
require 'iudex-rome' #FIXME

class TestFeedFilters < MiniTest::Unit::TestCase
#  include Iudex::Filter
  include Iudex::Core
  include Iudex::DA

  include Gravitext::HTMap

  import 'iudex.filter.core.FilterChain'
  import 'iudex.filter.core.MDCUnsetter'
  import 'iudex.filter.core.Switch'
  import 'iudex.filter.core.Selector'

  import 'iudex.core.ContentKeys'

  import 'iudex.core.filters.UHashMDCSetter'
  import 'iudex.core.filters.ContentFetcher'
  import 'iudex.core.filters.TextCtrlWSFilter'

  import 'iudex.httpclient3.HTTPClient3'

  import 'iudex.da.feed.FutureDateFilter'
  import 'iudex.da.feed.FeedWriter'

  import 'iudex.rome.RomeFeedParser'

  # FIXME: Where?  UniMap.define_accessors

  # FIXME: Example complete full filter configuration for testing.
  #        Likely this gets moved.

  def test_filter

    fcf = Iudex::Core::FilterChainFactory.new( "test-da" )
    fcf.add_summary_reporter #( 1.0 )
    fcf.add_by_filter_reporter #( 2.5 )

    def fcf.feed_post
      [ RomeFeedParser.new,
        feed_writer ]
    end

    def fcf.feed_writer
      f = FeedWriter.new( data_source, [] )

      create_chain( "feed-update", feed_update ) { |c| f.update_ref_filter = c }
      create_chain( "feed-new",    feed_new )    { |c| f.new_ref_filter = c }
      f
    end

    def fcf.feed_filters
      mf = RJack::HTTPClient3::ManagerFacade.new
      mf.start

      [ ContentFetcher.new( HTTPClient3.new( mf.client ),
                            create_chain( "feed-receiver", feed_post ) ) ]
    end

    def fcf.feed_update
      [ TextCtrlWSFilter.new( ContentKeys::TITLE ),
        FutureDateFilter.new( ContentKeys::PUB_DATE ) ]
    end

    def fcf.feed_new
      [ TextCtrlWSFilter.new( ContentKeys::TITLE ),
        FutureDateFilter.new( ContentKeys::PUB_DATE ) ]
    end

    def fcf.data_source
      factory = PoolDataSourceFactory.new #( 'loglevel' => 2 )
      factory.create
    end

    fcf.open
    fcf.close
  end

end
