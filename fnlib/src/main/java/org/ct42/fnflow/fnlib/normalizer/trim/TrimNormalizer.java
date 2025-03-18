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

package org.ct42.fnflow.fnlib.normalizer.trim;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.springframework.stereotype.Component;

/**
 * It is a function for trimming a text value in a JsonNode input object's specific path.
 *
 * @author Sajjad Safaeian
 */
@Component("trimNormalizer")
public class TrimNormalizer extends ConfigurableFunction<JsonNode, JsonNode, TrimProperties> {

    @Override
    public JsonNode apply(JsonNode input) {
        JsonPointer pointer = properties.getElementPath();
        JsonNode resultNode = input.at(pointer);

        if(resultNode.isTextual()) {
            JsonNode parentNode = input.at(pointer.head());
            if(parentNode.isObject()) {
                ((ObjectNode) parentNode).put(pointer.last().getMatchingProperty(), trimString(resultNode.asText()));
            }
        }

        if(resultNode.isArray()) {
            ArrayNode array = (ArrayNode) resultNode;
            for (int i = 0; i <array.size(); i++) {
                if(array.get(i).isTextual()) {
                    array.set(i, trimString(array.get(i).asText()));
                }
            }
        }

        return input;
    }

    private String trimString(String input) {
        return switch (properties.getMode()) {
            case RIGHT -> StringUtils.stripStart(input, null);
            case LEFT -> StringUtils.stripEnd(input, null);
            case BOTH -> StringUtils.strip(input);
        };
    }
}
