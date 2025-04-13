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
 * Represents a token for byte sizes (e.g., 10KB, 1GB).
 */
@PublicEvolving
public class ByteSize implements Token {

    private final long bytes;

    public ByteSize(String value) {
        this.bytes = parseByteSize(value);
    }

    private long parseByteSize(String value) {
        if (value.endsWith("KB")) {
            return Long.parseLong(value.replace("KB", "")) * 1024;
        } else if (value.endsWith("MB")) {
            return Long.parseLong(value.replace("MB", "")) * 1024 * 1024;
        } else if (value.endsWith("GB")) {
            return Long.parseLong(value.replace("GB", "")) * 1024 * 1024 * 1024;
        } else if (value.endsWith("TB")) {
            return Long.parseLong(value.replace("TB", "")) * 1024L * 1024 * 1024 * 1024;
        } else if (value.endsWith("B")) {
            return Long.parseLong(value.replace("B", ""));
        } else {
            throw new IllegalArgumentException("Invalid byte size: " + value);
        }
    }

    public long getBytes() {
        return bytes;
    }

    @Override
    public Object value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "BYTE_SIZE");
        json.addProperty("value", bytes);
        return json;
    }
}