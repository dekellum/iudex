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

    attr_accessor :domain_depth_divisor
    alias :host_depth_divisor  :domain_depth_divisor
    alias :host_depth_divisor= :domain_depth_divisor=

    attr_accessor :max_priority_urls

    attr_accessor :max_domain_urls
    alias :max_host_urls  :max_domain_urls
    alias :max_host_urls= :max_domain_urls=

    attr_accessor :max_urls

    attr_accessor :domain_group

    def initialize( data_source, mapper )
      super()

      @domain_depth_divisor = 8.0
      @domain_group = false

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

      q = filter_query( fields(
            ( :domain if ( domain_depth_divisor || domain_group ) ) ) )

      if domain_depth_divisor
        q = wrap_domain_partition_query( :priority,
                                         fields( ( :domain if domain_group ) ), q )
      end

      q = wrap_domain_group_query( fields, q ) if domain_group
      # FIXME: Make conditional, oft not needed

      q.gsub( /\s+/, ' ').strip
    end

    def wrap_domain_partition_query( priority_field, fields, sub )
      <<-SQL
        SELECT #{flds_d}
        FROM ( SELECT #{flds_d},
               ( #{priority_field} -
                 ( ( dpos - 1 ) / #{domain_depth_divisor} ) ) AS d_adj_priority
               FROM ( SELECT #{flds_d},
                             row_number() OVER (
                                PARTITION BY domain
                                ORDER BY priority DESC ) AS dpos
                      FROM ( #{ sub } ) AS subP
                     ) AS subH
                WHERE dpos <= #{max_domain_urls}
              ) AS subA
        ORDER BY d_adj_priority DESC
        LIMIT ?
      SQL
    end

    def filter_query( flds )
      criteria = [ "next_visit_after <= now()" ]

      # FIXME: uhash range criteria goes here
      # FIXME: age adjusted priority goes here

      sql = <<-SQL
        SELECT #{clist flds}
        FROM urls
        WHERE #{and_list criteria}
        ORDER BY priority DESC, next_visit_after DESC
      SQL

      # FIXME: DESC next_visit_after means take the youngest
      # first, so starvation influencing (opposite of aged priority)

      # FIXME: Order only if LIMITing?

      sql += " LIMIT #{max_priority_urls}" if max_priority_urls

      sql
    end

    def wrap_domain_group_query( flds, sub )
      <<-SQL
        SELECT #{clist flds}
        FROM ( #{sub} ) as subDG
        ORDER BY domain, priority DESC;
      SQL
    end

    #FIXME: Replace with clist
    def flds
      @mapper.field_names
    end

    #FIXME: Replace with clist
    def flds_d
      clist( fields( :domain ) )
    end

    def fields( *ksyms )
      @mapper.fields | keys( *( ksyms.compact ) )
    end

    def clist( l )
      l.compact.join( ', ' )
    end

    def and_list( l )
      l.compact.join( " AND " )
    end

  end

end
