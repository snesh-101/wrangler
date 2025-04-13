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

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

/**
 * This class represents a time duration token in the wrangler directives.
 * It parses and stores time duration values like "150ms", "2h", "1d" etc.
 * All durations are internally converted to milliseconds for consistency.
 */
@PublicEvolving
public class TimeDuration implements Token {
    private final long milliseconds;
    private final String originalValue;

    public TimeDuration(String value) {
        this.originalValue = value;
        this.milliseconds = parseTimeDuration(value);
    }

    private long parseTimeDuration(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Time duration cannot be null or empty");
        }

        String number = value.replaceAll("[^0-9]", "");
        String unit = value.replaceAll("[0-9]", "");

        if (number.isEmpty() || unit.isEmpty()) {
            throw new IllegalArgumentException("Invalid time duration format: " + value);
        }

        long numericValue = Long.parseLong(number);
        switch (unit) {
            case "ms":
                return numericValue;
            case "s":
                return numericValue * 1000;
            case "m":
                return numericValue * 60 * 1000;
            case "h":
                return numericValue * 60 * 60 * 1000;
            case "d":
                return numericValue * 24 * 60 * 60 * 1000;
            default:
                throw new IllegalArgumentException("Invalid time unit: " + unit);
        }
    }

    /**
     * Returns the time duration in milliseconds.
     *
     * @return duration in milliseconds
     */
    public long getMilliseconds() {
        return milliseconds;
    }

    @Override
    public Object value() {
        return milliseconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.TIME_DURATION.name());
        object.addProperty("value", originalValue);
        object.addProperty("milliseconds", milliseconds);
        return object;
    }
}