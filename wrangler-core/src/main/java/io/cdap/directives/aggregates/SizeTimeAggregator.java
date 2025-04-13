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

package io.cdap.directives.aggregates;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.lineage.Lineage;
import io.cdap.wrangler.api.lineage.Many;
import io.cdap.wrangler.api.lineage.Mutation;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;

import java.util.List;

/**
 * A directive for aggregating byte sizes and time durations from source
 * columns.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-size-time")
@Categories(categories = { "aggregate" })
@Description("Aggregates byte sizes and time durations from source columns into target columns")
public class SizeTimeAggregator implements Directive, Lineage {
    public static final String NAME = "aggregate-size-time";
    private static final String SIZE_TOTAL = "size_total";
    private static final String TIME_TOTAL = "time_total";

    // Column names from the arguments
    private String sizeSourceColumn;
    private String timeSourceColumn;
    private String sizeTotalColumn;
    private String timeTotalColumn;
    private String sizeUnit;
    private String timeUnit;
    private boolean isAverage;

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
        builder.define("sizeColumn", TokenType.COLUMN_NAME);
        builder.define("timeColumn", TokenType.COLUMN_NAME);
        builder.define("sizeTarget", TokenType.COLUMN_NAME);
        builder.define("timeTarget", TokenType.COLUMN_NAME);
        builder.define("sizeUnit", TokenType.TEXT, true); // Optional, e.g. 'MB', 'GB'
        builder.define("timeUnit", TokenType.TEXT, true); // Optional, e.g. 'seconds', 'minutes'
        builder.define("average", TokenType.BOOLEAN, true); // Optional, default false
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        this.sizeSourceColumn = ((ColumnName) args.value("sizeColumn")).value();
        this.timeSourceColumn = ((ColumnName) args.value("timeColumn")).value();
        this.sizeTotalColumn = ((ColumnName) args.value("sizeTarget")).value();
        this.timeTotalColumn = ((ColumnName) args.value("timeTarget")).value();

        if (args.contains("sizeUnit")) {
            this.sizeUnit = ((Text) args.value("sizeUnit")).value();
        }
        if (args.contains("timeUnit")) {
            this.timeUnit = ((Text) args.value("timeUnit")).value();
        }
        if (args.contains("average")) {
            this.isAverage = args.value("average").value().equals(true);
        } else {
            this.isAverage = false;
        }
    }

    @Override
    public void destroy() {
        // no-op
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        // Initialize transient store for accumulation if not done
        TransientStore store = context.getTransientStore();
        if (store.get(SIZE_TOTAL) == null) {
            store.set(TransientVariableScope.GLOBAL, SIZE_TOTAL, 0L);
            store.set(TransientVariableScope.GLOBAL, TIME_TOTAL, 0L);
            store.set(TransientVariableScope.GLOBAL, "record_count", 0L);
        }

        // Process each row, accumulating totals
        for (Row row : rows) {
            int sizeIdx = row.find(sizeSourceColumn);
            int timeIdx = row.find(timeSourceColumn);

            if (sizeIdx != -1 && timeIdx != -1) {
                Object sizeObj = row.getValue(sizeIdx);
                Object timeObj = row.getValue(timeIdx);

                try {
                    // Parse and accumulate byte size
                    if (sizeObj instanceof String) {
                        ByteSize size = new ByteSize((String) sizeObj);
                        store.increment(TransientVariableScope.GLOBAL, SIZE_TOTAL, size.getBytes());
                    }

                    // Parse and accumulate time duration
                    if (timeObj instanceof String) {
                        TimeDuration duration = new TimeDuration((String) timeObj);
                        store.increment(TransientVariableScope.GLOBAL, TIME_TOTAL, duration.getMilliseconds());
                    }

                    store.increment(TransientVariableScope.GLOBAL, "record_count", 1);
                } catch (IllegalArgumentException e) {
                    throw new DirectiveExecutionException(NAME,
                            String.format("Invalid format in row: size='%s', time='%s'", sizeObj, timeObj));
                }
            }
        }

        // On the last row, calculate final values and add to the row
        if (!rows.isEmpty()) {
            Row lastRow = rows.get(rows.size() - 1);
            long totalSize = store.get(SIZE_TOTAL);
            long totalTime = store.get(TIME_TOTAL);
            long recordCount = store.get("record_count");

            if (isAverage && recordCount > 0) {
                totalSize = totalSize / recordCount;
                totalTime = totalTime / recordCount;
            }

            // Convert to requested units if specified
            if (sizeUnit != null) {
                totalSize = convertBytes(totalSize, sizeUnit);
            }
            if (timeUnit != null) {
                totalTime = convertTime(totalTime, timeUnit);
            }

            lastRow.add(sizeTotalColumn, totalSize);
            lastRow.add(timeTotalColumn, totalTime);
        }

        return rows;
    }

    private long convertBytes(long bytes, String unit) {
        switch (unit.toUpperCase()) {
            case "KB":
                return bytes / 1024;
            case "MB":
                return bytes / (1024 * 1024);
            case "GB":
                return bytes / (1024 * 1024 * 1024);
            case "TB":
                return bytes / (1024L * 1024 * 1024 * 1024);
            default:
                return bytes;
        }
    }

    private long convertTime(long milliseconds, String unit) {
        switch (unit.toLowerCase()) {
            case "seconds":
                return milliseconds / 1000;
            case "minutes":
                return milliseconds / (60 * 1000);
            case "hours":
                return milliseconds / (60 * 60 * 1000);
            case "days":
                return milliseconds / (24 * 60 * 60 * 1000);
            default:
                return milliseconds;
        }
    }

    @Override
    public Mutation lineage() {
        return Mutation.builder()
                .readable("Aggregated %s and %s into %s and %s",
                        sizeSourceColumn, timeSourceColumn, sizeTotalColumn, timeTotalColumn)
                .all(Many.of(sizeSourceColumn, timeSourceColumn), Many.of(sizeTotalColumn, timeTotalColumn))
                .build();
    }
}