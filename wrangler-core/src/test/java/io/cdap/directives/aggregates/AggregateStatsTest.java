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
import io.cdap.wrangler.api.RecipeException;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests {@link AggregateStats}
 */
public class AggregateStatsTest {

    private static final double DELTA = 0.001;

    @Test
    public void testBasicAggregation() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size :total_time"
        };

        List<Row> rows = Arrays.asList(
                new Row("disk_size", 100L).add("process_time", 5L),
                new Row("disk_size", 200L).add("process_time", 8L),
                new Row("disk_size", 150L).add("process_time", 6L));

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(450L, ((Long) rows.get(0).getValue("total_size")).longValue());
        Assert.assertEquals(19L, ((Long) rows.get(0).getValue("total_time")).longValue());
    }

    @Test
    public void testSpecificationExample() throws Exception {
        // Test case exactly matching specification example
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_mb :total_time_sec 'MB' 'seconds'"
        };

        // Create sample log/transaction data
        // Using 1 MB = 1024 * 1024 bytes for consistency
        List<Row> rows = Arrays.asList(
                // 2.5 MB data, 1.5 seconds response time
                new Row("data_transfer_size", 2.5 * 1024 * 1024).add("response_time", 1.5 * 1000),
                // 1.8 MB data, 0.8 seconds response time
                new Row("data_transfer_size", 1.8 * 1024 * 1024).add("response_time", 0.8 * 1000),
                // 3.2 MB data, 2.1 seconds response time
                new Row("data_transfer_size", 3.2 * 1024 * 1024).add("response_time", 2.1 * 1000));

        List<Row> results = TestingRig.execute(recipe, rows);

        // Verify results
        Assert.assertEquals(1, results.size());
        // Expected: 2.5 + 1.8 + 3.2 = 7.5 MB
        Assert.assertEquals(7.5, ((Double) results.get(0).getValue("total_size_mb")).doubleValue(), DELTA);
        // Expected: 1.5 + 0.8 + 2.1 = 4.4 seconds
        Assert.assertEquals(4.4, ((Double) results.get(0).getValue("total_time_sec")).doubleValue(), DELTA);
    }

    @Test
    public void testAggregationWithUnits() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size_mb :total_time_min 'MB' 'minutes'"
        };

        List<Row> rows = Arrays.asList(
                new Row("disk_size", 1024L * 1024L).add("process_time", 120L), // 1MB, 2min
                new Row("disk_size", 2048L * 1024L).add("process_time", 180L) // 2MB, 3min
        );

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("total_size_mb")).doubleValue(), DELTA);
        Assert.assertEquals(5.0, ((Double) rows.get(0).getValue("total_time_min")).doubleValue(), DELTA);
    }

    @Test
    public void testAggregationWithAverage() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :avg_size :avg_time 'MB' 'minutes' true"
        };

        List<Row> rows = Arrays.asList(
                new Row("disk_size", 1024L * 1024L).add("process_time", 120L), // 1MB, 2min
                new Row("disk_size", 2048L * 1024L).add("process_time", 180L), // 2MB, 3min
                new Row("disk_size", 3072L * 1024L).add("process_time", 240L) // 3MB, 4min
        );

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(2.0, ((Double) rows.get(0).getValue("avg_size")).doubleValue(), DELTA);
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("avg_time")).doubleValue(), DELTA);
    }

    @Test
    public void testSmallValuesWithPrecision() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size_kb :total_time_ms 'KB' 'milliseconds'"
        };

        // Test with small values to verify precision
        List<Row> rows = Arrays.asList(
                new Row("disk_size", 1024L).add("process_time", 100L), // 1KB, 100ms
                new Row("disk_size", 2048L).add("process_time", 150L) // 2KB, 150ms
        );

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("total_size_kb")).doubleValue(), DELTA);
        Assert.assertEquals(250.0, ((Double) rows.get(0).getValue("total_time_ms")).doubleValue(), DELTA);
    }

    @Test
    public void testEmptyInput() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size :total_time"
        };

        List<Row> rows = Collections.emptyList();
        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(0L, ((Long) rows.get(0).getValue("total_size")).longValue());
        Assert.assertEquals(0L, ((Long) rows.get(0).getValue("total_time")).longValue());
    }

    @Test(expected = RecipeException.class)
    public void testMissingColumn() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :missing_size :process_time :total_size :total_time"
        };

        List<Row> rows = Collections.singletonList(
                new Row("disk_size", 100L).add("process_time", 5L));

        TestingRig.execute(directives, rows);
    }

    @Test(expected = RecipeException.class)
    public void testInvalidInputType() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size :total_time"
        };

        List<Row> rows = Collections.singletonList(
                new Row("disk_size", "not a number").add("process_time", 5L));

        TestingRig.execute(directives, rows);
    }

    @Test(expected = RecipeException.class)
    public void testInvalidTimeUnit() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size :total_time 'MB' 'invalid_unit'"
        };

        List<Row> rows = Collections.singletonList(
                new Row("disk_size", 100L).add("process_time", 5L));

        TestingRig.execute(directives, rows);
    }

    @Test
    public void testLargeNumbers() throws Exception {
        String[] directives = new String[] {
                "aggregate-size-time :disk_size :process_time :total_size_tb :total_time_hr 'TB' 'hours'"
        };

        // Test with large numbers to verify no overflow
        List<Row> rows = Arrays.asList(
                new Row("disk_size", 1024L * 1024L * 1024L * 1024L).add("process_time", 3600L), // 1TB, 1hr
                new Row("disk_size", 2048L * 1024L * 1024L * 1024L).add("process_time", 7200L) // 2TB, 2hr
        );

        rows = TestingRig.execute(directives, rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("total_size_tb")).doubleValue(), DELTA);
        Assert.assertEquals(3.0, ((Double) rows.get(0).getValue("total_time_hr")).doubleValue(), DELTA);
    }
}