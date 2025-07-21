/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ct42.fnflow.batchdlt;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

/**
 * Holds all data of one element of a batch.
 * This is a DTO for in and out
 *
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@Getter
public class BatchElement {
    private final JsonNode input;
    private JsonNode output;
    private Throwable error;
    private Integer inputIndex;

    public BatchElement(JsonNode input) {
        this.input = input;
    }

    public BatchElement(JsonNode input, Integer inputIndex) {
        this.input = input;
        this.inputIndex = inputIndex;
    }

    public void processWithOutput(JsonNode output) {
        this.output = output;
    }

    public void processWithError(Throwable error) {
        this.error = error;
    }


}
