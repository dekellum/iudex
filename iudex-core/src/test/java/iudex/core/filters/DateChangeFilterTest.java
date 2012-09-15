/*
 * Copyright (c) 2008-2012 Matt Sanford
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package iudex.core.filters;

import iudex.core.ContentKeys;

import java.util.Date;

import static org.junit.Assert.*;

import org.junit.Test;

import com.gravitext.htmap.UniMap;

public class DateChangeFilterTest {

    @Test
    public void testSetChangeCutoffPositive() {
        DateChangeFilter filter = new DateChangeFilter(true);
        filter.setChangeCutoff(100L);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetChangeCutoffNegative() {
        DateChangeFilter filter = new DateChangeFilter(true);
        filter.setChangeCutoff(-100L);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetChangeCutoffZero() {
        DateChangeFilter filter = new DateChangeFilter(true);
        filter.setChangeCutoff(0L);
    }

    @Test
    public void testFilterDisabled() {
        UniMap content = new UniMap();
        UniMap priorInfo = new UniMap();
        priorInfo.put( ContentKeys.REF_PUB_DATE, new Date(0));
        content.put( ContentKeys.REF_PUB_DATE, new Date(System.currentTimeMillis()));
        content.put( ContentKeys.CURRENT, priorInfo );

        DateChangeFilter filter = new DateChangeFilter(false);
        boolean accept = filter.filter(content);
        assertEquals( true, accept );
    }

    @Test
    public void testFilterEnabled() {
        UniMap content = new UniMap();
        UniMap priorInfo = new UniMap();
        priorInfo.put( ContentKeys.REF_PUB_DATE, new Date(0));
        content.put( ContentKeys.REF_PUB_DATE, new Date(System.currentTimeMillis()));
        content.put( ContentKeys.CURRENT, priorInfo );

        DateChangeFilter filter = new DateChangeFilter(false);
        boolean accept = filter.filter(content);
        assertEquals( true, accept );
        assertNotNull( "REF_PUB_DELTA should be set", content.get( ContentKeys.REF_PUB_DELTA ) );
    }

    @Test
    public void testFilterEnabledNoDiff() {
        UniMap content = new UniMap();
        UniMap priorInfo = new UniMap();
        priorInfo.put( ContentKeys.REF_PUB_DATE, new Date(0));
        content.put( ContentKeys.REF_PUB_DATE, new Date(0));
        content.put( ContentKeys.CURRENT, priorInfo );

        DateChangeFilter filter = new DateChangeFilter(false);
        boolean accept = filter.filter(content);
        Float delta = content.get( ContentKeys.REF_PUB_DELTA );

        assertEquals( "Should accept content not explicitly failing the filter criteria",  true, accept );
        assertNotNull( "REF_PUB_DELTA should be set", delta );
        assertEquals( "Expected a zero difference", 0.0f, delta.floatValue(), 0.0001f );
    }

    @Test
    public void testFilterEnabledNoPriorDate() {
        UniMap content = new UniMap();
        UniMap priorInfo = new UniMap();
        content.put( ContentKeys.REF_PUB_DATE, new Date(0));
        content.put( ContentKeys.CURRENT, priorInfo );

        DateChangeFilter filter = new DateChangeFilter(false);
        boolean accept = filter.filter(content);

        assertEquals( "Should accept content not explicitly failing the filter criteria",  true, accept );
        assertNull( "REF_PUB_DELTA should not be set", content.get( ContentKeys.REF_PUB_DELTA ) );
    }

    @Test
    public void testFilterEnabledNoNewDate() {
        UniMap content = new UniMap();
        UniMap priorInfo = new UniMap();
        priorInfo.put( ContentKeys.REF_PUB_DATE, new Date(0));
        content.put( ContentKeys.CURRENT, priorInfo );

        DateChangeFilter filter = new DateChangeFilter(false);
        boolean accept = filter.filter(content);

        assertEquals( "Should accept content not explicitly failing the filter criteria",  true, accept );
        assertNull( "REF_PUB_DELTA should not be set", content.get( ContentKeys.REF_PUB_DELTA ) );
    }

}
