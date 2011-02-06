#--
# Copyright (c) 2008-2011 David Kellum
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
require 'iudex-da/pool_data_source_factory'

module Iudex::DA

  class Importer
    include Iudex::Core
    include Gravitext::HTMap

    include Iudex::Filter::KeyHelper

    import 'iudex.da.BaseTransformer'
    import 'iudex.da.ContentUpdater'

    def initialize()
      @dsf = PoolDataSourceFactory.new
      UniMap.define_accessors

      Hooker.apply( [ :iudex, :importer ], self )
    end

    def import_files( files = ARGV )
      files.each do |fname|
        open( fname, "r" ) do |fin|
          import( fin )
        end
      end
    end

    def import( input )

      cmapper = ContentMapper.new( keys( import_keys ) )
      transformer = BaseTransformer.new
      updater = ContentUpdater.new( @dsf.create, cmapper, transformer )

      tmpl = template_map
      batch = []

      input.each do |line|
        umap = tmpl.clone
        parse_to( line, umap )
        batch << umap
        if batch.length >= 1_000
          updater.update( batch )
          batch.clear
         end
      end
      updater.update( batch ) unless batch.empty?
    end

    def import_keys
      [ :uhash, :host, :url, :type, :priority, :next_visit_after ]
    end

    def template_map
      umap = UniMap.new
      umap.type = "FEED"
      umap.next_visit_after = Time.now
      umap.priority = 1.0
      umap
    end

    def parse_to( line, umap )
      fields = line.split( ',' )
      umap.url = VisitURL.normalize( fields[0] )
      umap
    end

    def close
      @dsf.close if @dsf
      @dsf = nil
    end
  end
end
