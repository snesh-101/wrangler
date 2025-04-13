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
 * Tests {@link ByteSize} parsing and value retrieval
 */
public class ByteSizeTest {

    @Test
    public void testSimpleByteValues() {
        Assert.assertEquals(1024L, new ByteSize("1KB").getBytes());
        Assert.assertEquals(1024L * 1024L, new ByteSize("1MB").getBytes());
        Assert.assertEquals(1024L * 1024L * 1024L, new ByteSize("1GB").getBytes());
        Assert.assertEquals(1024L * 1024L * 1024L * 1024L, new ByteSize("1TB").getBytes());
        Assert.assertEquals(123L, new ByteSize("123B").getBytes());
    }

    @Test
    public void testDecimalByteValues() {
        Assert.assertEquals(1536L, new ByteSize("1.5KB").getBytes()); // 1.5 * 1024
        Assert.assertEquals(2560L, new ByteSize("2.5KB").getBytes()); // 2.5 * 1024
        Assert.assertEquals(1572864L, new ByteSize("1.5MB").getBytes()); // 1.5 * 1024 * 1024
    }

    @Test
    public void testCaseInsensitiveUnits() {
        Assert.assertEquals(1024L, new ByteSize("1kb").getBytes());
        Assert.assertEquals(1024L, new ByteSize("1KB").getBytes());
        Assert.assertEquals(1024L * 1024L, new ByteSize("1mb").getBytes());
        Assert.assertEquals(1024L * 1024L, new ByteSize("1MB").getBytes());
    }

    @Test
    public void testWhitespaceHandling() {
        Assert.assertEquals(1024L, new ByteSize("1 KB").getBytes());
        Assert.assertEquals(1024L, new ByteSize(" 1KB").getBytes());
        Assert.assertEquals(1024L, new ByteSize("1KB ").getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNumber() {
        new ByteSize("notANumber KB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnit() {
        new ByteSize("1 XB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingUnit() {
        new ByteSize("1024");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyInput() {
        new ByteSize("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullInput() {
        new ByteSize(null);
    }
}