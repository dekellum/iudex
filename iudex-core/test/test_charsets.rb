#!/usr/bin/env jruby
# -*- coding: utf-8 -*-
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

class TestCharsets < MiniTest::Unit::TestCase
  include Gravitext::HTMap

  import 'iudex.util.Charsets'

  UniMap.define_accessors

  def test_default
    assert_equal( Charsets::WINDOWS_1252, Charsets.default_charset )
  end

  # Test all mappings from the whatwg guidelines that are supported by the JVM.
  #
  # @see http://www.whatwg.org/specs/web-apps/current-work/multipage/parsing.html#character-encodings-0
  def test_expand
    mapping = {
      Charsets::EUC_KR => Charsets::WINDOWS_949,
      Charsets::GB2312 => Charsets::GBK,
      Charsets::ISO_8859_1 => Charsets::WINDOWS_1252,
      Charsets::ISO_8859_9 => Charsets::WINDOWS_1254,
      Charsets::ISO_8859_11 => Charsets::WINDOWS_874,
      Charsets::KS_C_5601_1987 => Charsets::WINDOWS_949,
      Charsets::SHIFT_JIS => Charsets::WINDOWS_31J,
      Charsets::TIS_620 => Charsets::WINDOWS_874,
      Charsets::ASCII => Charsets::WINDOWS_1252,
    }

    mapping.each_pair do |map_from, map_to|
      assert_equal( map_to, Charsets.expand(map_from), "#{map_from} should map to #{map_to}" )
    end
  end

end
