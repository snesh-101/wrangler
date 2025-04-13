/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link TimeDuration} parsing and value retrieval
 */
public class TimeDurationTest {

    @Test
    public void testSimpleTimeValues() {
        Assert.assertEquals(1000L, new TimeDuration("1s").getMilliseconds());
        Assert.assertEquals(60000L, new TimeDuration("1m").getMilliseconds());
        Assert.assertEquals(3600000L, new TimeDuration("1h").getMilliseconds());
        Assert.assertEquals(86400000L, new TimeDuration("1d").getMilliseconds());
        Assert.assertEquals(123L, new TimeDuration("123ms").getMilliseconds());
    }

    @Test
    public void testDecimalTimeValues() {
        Assert.assertEquals(1500L, new TimeDuration("1.5s").getMilliseconds());
        Assert.assertEquals(2500L, new TimeDuration("2.5s").getMilliseconds());
        Assert.assertEquals(90000L, new TimeDuration("1.5m").getMilliseconds());
        Assert.assertEquals(5400000L, new TimeDuration("1.5h").getMilliseconds());
    }

    @Test
    public void testCaseInsensitiveUnits() {
        Assert.assertEquals(1000L, new TimeDuration("1S").getMilliseconds());
        Assert.assertEquals(1000L, new TimeDuration("1s").getMilliseconds());
        Assert.assertEquals(60000L, new TimeDuration("1M").getMilliseconds());
        Assert.assertEquals(60000L, new TimeDuration("1m").getMilliseconds());
    }

    @Test
    public void testWhitespaceHandling() {
        Assert.assertEquals(1000L, new TimeDuration("1 s").getMilliseconds());
        Assert.assertEquals(1000L, new TimeDuration(" 1s").getMilliseconds());
        Assert.assertEquals(1000L, new TimeDuration("1s ").getMilliseconds());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNumber() {
        new TimeDuration("notANumber s");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnit() {
        new TimeDuration("1 x");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingUnit() {
        new TimeDuration("1000");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyInput() {
        new TimeDuration("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullInput() {
        new TimeDuration(null);
    }
}