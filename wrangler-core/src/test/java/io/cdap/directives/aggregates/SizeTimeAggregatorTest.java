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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.directives.aggregates;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveLoadException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.RecipeException;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests {@link SizeTimeAggregator}
 */
public class SizeTimeAggregatorTest {

    @Test
    public void testBasicAggregation() throws DirectiveParseException, DirectiveExecutionException,
            DirectiveLoadException, RecipeException {
        String[] directive = new String[] {
                "aggregate-size-time :size :time :total_size :total_time"
        };

        List<Row> rows = Arrays.asList(
                new Row("size", "1KB").add("time", "1s"),
                new Row("size", "2KB").add("time", "2s"),
                new Row("size", "3KB").add("time", "3s"));

        List<Row> results = TestingRig.execute(directive, rows);

        // Check that all rows are preserved
        Assert.assertEquals(3, results.size());

        // Check that aggregated values are added to last row
        Row lastRow = results.get(results.size() - 1);
        long totalSize = (Long) lastRow.getValue("total_size");
        long totalTime = (Long) lastRow.getValue("total_time");

        // 6KB = 6 * 1024 bytes
        Assert.assertEquals(6 * 1024L, totalSize);
        // 6s = 6000 milliseconds
        Assert.assertEquals(6000L, totalTime);
    }

    @Test
    public void testAggregationWithUnitConversion() throws DirectiveParseException, DirectiveExecutionException,
            DirectiveLoadException, RecipeException {
        String[] directive = new String[] {
                "aggregate-size-time :size :time :total_size_mb :total_time_min 'MB' 'minutes'"
        };

        List<Row> rows = Arrays.asList(
                new Row("size", "1MB").add("time", "30s"),
                new Row("size", "2MB").add("time", "90s"));

        List<Row> results = TestingRig.execute(directive, rows);
        Row lastRow = results.get(results.size() - 1);

        long totalSizeMB = (Long) lastRow.getValue("total_size_mb");
        long totalTimeMin = (Long) lastRow.getValue("total_time_min");

        Assert.assertEquals(3L, totalSizeMB); // 3 MB
        Assert.assertEquals(2L, totalTimeMin); // 120s = 2 min
    }

    @Test
    public void testAggregationWithAverages() throws DirectiveParseException, DirectiveExecutionException,
            DirectiveLoadException, RecipeException {
        String[] directive = new String[] {
                "aggregate-size-time :size :time :avg_size :avg_time 'MB' 'seconds' true"
        };

        List<Row> rows = Arrays.asList(
                new Row("size", "1MB").add("time", "1000ms"),
                new Row("size", "2MB").add("time", "2000ms"),
                new Row("size", "3MB").add("time", "3000ms"));

        List<Row> results = TestingRig.execute(directive, rows);
        Row lastRow = results.get(results.size() - 1);

        long avgSizeMB = (Long) lastRow.getValue("avg_size");
        long avgTimeSec = (Long) lastRow.getValue("avg_time");

        Assert.assertEquals(2L, avgSizeMB); // (1+2+3)/3 = 2 MB
        Assert.assertEquals(2L, avgTimeSec); // (1+2+3)/3 = 2 seconds
    }

    @Test(expected = DirectiveExecutionException.class)
    public void testInvalidByteSize() throws DirectiveParseException, DirectiveExecutionException,
            DirectiveLoadException, RecipeException {
        String[] directive = new String[] {
                "aggregate-size-time :size :time :total_size :total_time"
        };

        List<Row> rows = Arrays.asList(
                new Row("size", "invalid").add("time", "1s"));

        TestingRig.execute(directive, rows);
    }

    @Test(expected = DirectiveExecutionException.class)
    public void testInvalidTimeDuration() throws DirectiveParseException, DirectiveExecutionException,
            DirectiveLoadException, RecipeException {
        String[] directive = new String[] {
                "aggregate-size-time :size :time :total_size :total_time"
        };

        List<Row> rows = Arrays.asList(
                new Row("size", "1KB").add("time", "invalid"));

        TestingRig.execute(directive, rows);
    }
}