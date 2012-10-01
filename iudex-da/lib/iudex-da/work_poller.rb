#--
# Copyright (c) 2008-2012 David Kellum
#
# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License.  You may
# obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.  See the License for the specific language governing
# permissions and limitations under the License.
#++

require 'iudex-da'
require 'iudex-da/key_helper'
require 'rjack-slf4j'

module Iudex::DA

  class WorkPoller < Java::iudex.core.GenericWorkPollStrategy
    include Iudex::Filter::KeyHelper

    import 'iudex.da.ContentReader'
    import 'java.sql.SQLException'
    import 'java.lang.RuntimeException'

    attr_accessor :host_depth_divisor
    attr_accessor :max_priority_urls
    attr_accessor :max_domain_urls
    attr_accessor :max_urls

    alias :max_host_urls  :max_domain_urls
    alias :max_host_urls= :max_domain_urls=

    def initialize( data_source, mapper )
      super()

      @host_depth_divisor =     8.0

      @max_priority_urls  = 500_000
      @max_domain_urls    =  10_000
      @max_urls           =  50_000

      @log = RJack::SLF4J[ self.class ]
      #FIXME: Add accessor for log in GenericWorkPollStrategy

      keys( :url, :priority, :next_visit_after ).each do |k|
        unless mapper.fields.include?( k )
          raise "WorkPoller needs mapper with #{key.name} included."
        end
      end

      @mapper = mapper
      @reader = ContentReader.new( data_source, mapper )
    end

    # Override GenericWorkPollStrategy
    def pollWorkImpl( visit_queue )
      visit_queue.add_all( poll )
    rescue SQLException => x
      @log.error( "On poll: ", x )
    end

    def poll
      query = generate_query
      @reader.select( query, max_urls )
    end

    def generate_query
      sql = <<-SQL
    SELECT #{flds}
    FROM ( SELECT #{flds_d}
           FROM ( SELECT #{flds_d},
                  ( priority - ( ( dpos - 1 ) / #{host_depth_divisor} ) ) AS adj_priority
                  FROM ( SELECT #{flds_d},
                                row_number() OVER ( PARTITION BY domain
                                                    ORDER BY priority DESC ) AS dpos
                         FROM ( SELECT #{flds_d}
                                FROM urls
                                WHERE next_visit_after <= now()
                                ORDER BY priority DESC, next_visit_after DESC
                                LIMIT #{max_priority_urls}
                              ) AS subP
                       ) AS subH
                  WHERE dpos <= #{max_domain_urls}
                ) AS subA
          ORDER BY adj_priority DESC
          LIMIT ? ) AS subF
    ORDER BY domain, priority DESC;
      SQL

      sql.gsub( /\s+/, ' ').strip
    end

    def flds
      @mapper.field_names
    end

    def flds_d
      ( @mapper.fields | [ :domain.to_k ] ).join( ', ' )
    end

  end

end
