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

package org.ct42.fnflow.fnlib.reducer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.ct42.fnflow.fnlib.NullProperties;
import org.springframework.stereotype.Component;

/**
 * @author Claas Thiele
 */
@Component("Reduce2One")
public class Reduce2One extends ConfigurableFunction<JsonNode, JsonNode, NullProperties> {
    @Override
    public JsonNode apply(JsonNode input) {
        JsonNode matches = input.get("matches");
        if(matches == null || !matches.isArray()) {
            throw new IllegalArgumentException("Invalid input, no matches array found");
        }
        if(matches.size() == 1) { // match
            //leave everything as it is
        } else if(matches.isEmpty()) { //no match
            ((ArrayNode)matches).add(JsonNodeFactory.instance.objectNode());
        } else { // ambiguous match
            ((ObjectNode)input).replace("input", JsonNodeFactory.instance.nullNode());
            ((ObjectNode)input).replace("matches", JsonNodeFactory.instance.arrayNode());
        }
        return input;
    }
}
