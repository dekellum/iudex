#--
# Copyright (c) 2008-2013 David Kellum
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

  # A SQL based WorkPoller
  class WorkPoller < Java::iudex.core.GenericWorkPollStrategy
    include Iudex::Filter::KeyHelper
    include Gravitext::HTMap

    import 'java.sql.SQLException'

    # If set > 0.0 group by domain and reduce priority for subsequent
    # urls within a common (registration level) domain (coefficient of
    # depth).  This increases crawl throughput when many domains are
    # available. (default: nil, off)
    attr_accessor :domain_depth_coef

    def domain_depth?
      domain_depth_coef && domain_depth_coef > 0.0
    end

    # Deprecated, use #domain_depth_coef (the reciprocal)
    def host_depth_divisor
      1.0 / domain_depth_coef
    end

    # Deprecated, use #domain_depth_coef= (reciprocal)
    def host_depth_divisor=( dv )
      @domain_depth_coef = 1.0 / dv
    end

    # If #domain_depth_coef is set, this sets maximum urls for any
    # single (registration level) domain (default: 10_000)
    attr_accessor :max_domain_urls

    # Deprecated, use #max_domain_urls
    alias :max_host_urls  :max_domain_urls

    # Deprecated, use #max_domain_urls=
    alias :max_host_urls= :max_domain_urls=

    # The limit of urls to obtain in a single poll (across all
    # domains) (default: 50_000)
    attr_accessor :max_urls

    # A secondary limit on the number of urls to consider, taking the
    # N high basic priority urls. This is only ever applied when
    # #domain_depth_coef is set. (default: nil, off)
    attr_accessor :max_priority_urls

    # If set true, provide the final work list ordered in domain,
    # priority order (default: false)
    attr_writer :do_domain_group

    # If set true, UPDATE reserved date (and instance, if specified)
    attr_writer :do_reserve

    # If set true, discards old queue at every poll, even if
    # do_reserve could make queue re-fill a safe operation.
    attr_writer :do_discard

    # The maximum ratio of current to max_urls where the old queue
    # will be discarded as a safety to avoid starvation (Default: 0.667)
    attr_accessor :max_discard_ratio

    def domain_group?
      @do_domain_group
    end

    def reserve?
      @do_reserve
    end

    def discard?
      @do_discard
    end

    # String uniquely identifying this worker instance. Only used here
    # with do_reserve.
    attr_accessor :instance

    # First age coefficient. If set > 0.0, adjust priority by the
    # equation:
    #
    #   priority + age_coef_1 * sqrt( age_coef_2 * age )
    #
    # Where age is now - next_visit_after the (default: 0.2)
    attr_accessor :age_coef_1

    # Second age coefficient (default: 0.1)
    attr_accessor :age_coef_2

    def aged_priority?
      ( age_coef_1 && age_coef_1 > 0.0 &&
        age_coef_2 && age_coef_2 > 0.0 )
    end

    # A table of option rows as defined below. A nil/unspecified
    # domain and type row applies to all domains/types not covered by
    # another row. Without such a row, work is limited to the explicit
    # domains/types listed.
    #
    # ==== Options
    #
    # :domain:: The registration-level, normalized lower-case domain
    #           value.
    #
    # :type:: An (upper-case) TYPE value to be AND'd with a domain
    #         domain or may appear on its own, applying to all
    #         unconfigured domains.
    #
    # :max:: The maximum number of visit urls to obtain in one poll
    #        (instead of the top level #max_urls.) A zero max_urls
    #        value excludes this domain/type (efficiently).
    #
    # Also a [ domain, max ] alternative syntax is currently supported
    # but deprecated.
    #
    attr_accessor :domain_union

    # An array containing a zero-based position and a total number of
    # evenly divided segments within the range of possible uhash
    # values. If set only work with uhashes in the designated range
    # will be polled. Note that the uhash is independent of domain,
    # being a hash on the entire URL. (default: nil, off)
    attr_accessor :uhash_slice

    def initialize( data_source, mapper )
      super()

      @domain_depth_coef  = nil
      @do_domain_group    = false
      @do_reserve         = false
      @do_discard         = true
      @instance           = nil

      @max_priority_urls  =    nil
      @max_domain_urls    = 10_000
      @max_urls           = 50_000
      @max_discard_ratio  = 2.0/3.0

      @age_coef_1         = 0.2
      @age_coef_2         = 0.1

      @domain_union       = []

      @uhash_slice        = nil

      @log = RJack::SLF4J[ self.class ]

      keys( :url, :priority, :next_visit_after ).each do |k|
        unless mapper.fields.include?( k )
          raise "WorkPoller needs mapper with #{key.name} included."
        end
      end

      @mapper = mapper
      @data_source = data_source
    end

    # Override GenericWorkPollStrategy
    def log
      @log.java_logger
    end

    # Override GenericWorkPollStrategy
    def pollWorkImpl( visit_queue )
      visit_queue.add_all( poll( visit_queue.order_count ) )
    rescue SQLException => x
      @log.error( "On poll: ", x )
    end

    # Poll work and return as List<UniMap>
    # Raises SQLException
    def poll( current_urls = 0 )
      query = generate_query( current_urls )
      @log.debug { "Poll query: #{query}" }
      reader.select_with_retry( query )
    end

    # Override GenericWorkPollStrategy to discard old VisitQueue
    # contents when do_reserve is enabled.
    def discard( visit_queue )
      if reserve? && visit_queue.order_count > 0
        orders = visit_queue.hosts.inject( [] ) do |a, hq|
          a.concat( hq.orders.to_a )
        end
        n = reader.unreserve( orders )
        @log.info { "Unreserved #{n} orders on discard" }
      end
    rescue SQLException => x
      @log.error( "On discard: ", x )
    end

    # Unreserve any orders that are reserved by the current instance.
    # No-op unless do_reserve and instance are set.
    def instance_unreserve
      if reserve? && instance
        n = reader.update( <<-SQL )
          UPDATE urls
          SET reserved = NULL
          WHERE reserved IS NOT NULL AND
                instance = '#{instance}'
        SQL
        @log.info { "Unreserved #{n} orders for instance #{instance}" }
        n
      end
    rescue SQLException => x
      @log.error( "On instance_unreserve: ", x )
    end

    def reader
      @reader ||= ContentReader.new( @data_source, @mapper ).tap do |r|
        r.priority_adjusted = aged_priority?
        r.max_retries = 10
      end
    end

    def domain_union=( table )
      @domain_union = table.map do | *args |
        args = args.flatten.dup
        opts = args.last.is_a?( Hash ) ? args.pop.dup : {}
        opts[ :domain ] ||= args.shift
        opts[ :max ]    ||= args.shift
        opts
      end
    end

    def domain_union?
      !@domain_union.empty?
    end

    def generate_query( current_urls )
      criteria = [ "next_visit_after <= now()" ]

      criteria << "reserved IS NULL" if reserve?

      if uhash_slice
        min, max = url64_range( *uhash_slice )
        criteria << "uhash > ( '#{min}' COLLATE \"C\" )" if min
        criteria << "uhash < ( '#{max}' COLLATE \"C\" )" if max
      end

      unless domain_union?
        query = generate_query_inner( criteria, ( max_urls - current_urls ) )
      else
        subqueries = []
        @domain_union.each do | opts |
          opts = opts.dup
          if opts[ :max ]
            opts[ :max ] = ( opts[ :max ] * ( max_urls - current_urls ) /
                             max_urls.to_f ).floor
          else
            opts[ :max ] = ( max_urls - current_urls )
          end

          next if opts[ :max ] == 0

          c = criteria.dup
          if opts[ :domain ].nil?
            c += @domain_union.map { |r| r[ :domain ] }.
                               compact.
                               uniq.
                               map { |nd| "domain != '#{nd}'" }
          else
            c << "domain = '#{opts[ :domain ]}'"
          end

          if opts[ :type ].nil?
            c += @domain_union.select { |r| r[ :domain ] == opts[ :domain ] }.
                               map { |r| r[ :type ] }.
                               compact.
                               uniq.
                               map { |nt| "type != '#{nt}'" }
          elsif opts[ :type ]
            c << "type = '#{opts[ :type ]}'"
          end

          subqueries << generate_query_inner( c, opts[ :max ] )
        end
        if subqueries.size == 1
          query = subqueries.first
        else
          query = "(" + subqueries.join( ") UNION ALL (" ) + ")"
        end
      end

      query = wrap_with_update( fields, query ) if reserve?

      query = wrap_domain_group_query( fields, query ) if domain_group?

      query = query.gsub( /\s+/, ' ').strip

      query
    end

    def generate_query_inner( criteria, max_urls )

      query = filter_query(
            fields( ( :domain if domain_depth? || domain_group? ),
                    ( :uhash if reserve? ) ),
            ( max_priority_urls if domain_depth? ),
            criteria )

      if domain_depth?
        flds = fields( ( :domain if domain_group? ) )
        query = wrap_domain_partition_query( flds, query )
      end

      limit_priority = domain_depth? ? :adj_priority : :priority
      query += <<-SQL
        ORDER BY #{limit_priority} DESC
        LIMIT #{max_urls}
      SQL

      query
    end

    def wrap_domain_partition_query( flds, sub )
      <<-SQL
        SELECT #{clist flds}
        FROM ( SELECT #{clist flds},
               ( priority - ( #{domain_depth_coef}::REAL * ( dpos - 1 ) )
               )::REAL AS adj_priority
               FROM ( SELECT #{clist flds},
                             row_number() OVER (
                               PARTITION BY domain
                               ORDER BY priority DESC ) AS dpos
                      FROM ( #{ sub } ) AS subP
                    ) AS subH
               WHERE dpos <= #{max_domain_urls}
             ) AS subA
      SQL
    end

    def filter_query( flds, max, criteria )

      if aged_priority?
        flds = flds.dup
        i = flds.index( :priority ) || flds.size
        flds[ i ] = <<-SQL.strip
          ( priority +
            #{age_coef_1}::REAL *
                  SQRT( #{age_coef_2}::REAL *
                        EXTRACT( EPOCH FROM ( now() - next_visit_after ) ) )::REAL
          ) AS priority
        SQL
      end

      sql = <<-SQL
        SELECT #{clist flds}
        FROM urls
        WHERE #{and_list criteria}
      SQL

      sql += <<-SQL if max
        ORDER BY priority DESC
        LIMIT #{max}
      SQL

      sql
    end

    def wrap_with_update( flds, sub )
      sflds = [ "reserved = now()" ]
      sflds << "instance = '#{instance}'" if instance

      # Use ..FOR UPDATE unless not supported by query specific
      # options with PostgreSQL <= 9.1
      sub += " FOR UPDATE" unless domain_depth? || domain_union?

      <<-SQL
        WITH work AS ( #{sub} ),
        reserve AS (
          UPDATE urls
          SET #{clist sflds}
          WHERE uhash IN ( SELECT uhash FROM work ) )
        SELECT #{clist flds} FROM work
      SQL
    end

    def wrap_domain_group_query( flds, sub )
      <<-SQL
        SELECT #{clist flds}
        FROM ( #{sub} ) AS subDG
        ORDER BY domain, priority DESC
      SQL
    end

    # URL 64 lexicon, ASCII or "C" LOCALE ordered
    URL64_ORDER = "-0123456789ABCDEFGHIJKLMNOPQRSTU" +
                  "VWXYZ_abcdefghijklmnopqrstuvwxyz"

    # Given a zero-based position within some number of segments,
    # returns [ min, max ] bounds where min will be nil at pos=0, and
    # max will be nil at pos=segments-1. Non nil values are uhash
    # prefixes that can be used as selection criteria.
    def url64_range( pos, segments )
      unless pos >= 0 && segments > pos
        raise "Invalid url64_range: 0 <= #{pos} < #{segments}"
      end

      period = ( 64 * 64 / segments.to_f )
      low  = ( period *  pos    ).round if  pos > 0
      high = ( period * (pos+1) ).round if (pos+1) < segments

      [ low, high ].map do |i|
        URL64_ORDER[ i / 64 ].chr + URL64_ORDER[ i % 64 ].chr if i
      end
    end

    def fields( *ksyms )
      ( @mapper.fields.map { |k| k.name.to_sym } |
        ksyms.flatten.compact.map { |s| s.to_sym } )
    end

    def clist( l )
      l.compact.join( ', ' )
    end

    def and_list( l )
      l.compact.join( " AND " )
    end

  end

end
