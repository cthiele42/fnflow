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

package org.ct42.fnflow.fnlib.normalizer.pad;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.springframework.stereotype.Component;

/**
 * It is a function for padding a text value in a JsonNode input object's specific path.
 *
 * @author Sajjad Safaeian
 */
@Component("padNormalizer")
public class PadNormalizer extends ConfigurableFunction<JsonNode, JsonNode, PadProperties> {

    @Override
    public JsonNode apply(JsonNode input) {
        JsonPointer pointer = properties.getElementPath();
        JsonNode resultNode = input.at(pointer);

        if(resultNode.isString()) {
            JsonNode parentNode = input.at(pointer.head());
            if(parentNode.isObject()) {
                ((ObjectNode) parentNode).put(pointer.last().getMatchingProperty(), padString(resultNode.asString()));
            }
        }

        if(resultNode.isArray()) {
            ArrayNode array = (ArrayNode) resultNode;
            for (int i = 0; i <array.size(); i++) {
                if(array.get(i).isString()) {
                    array.set(i, padString(array.get(i).asString()));
                }
            }
        }

        return input;
    }

    private String padString(String input) {
        return switch (properties.getPad()) {
            case LEFT -> StringUtils.leftPad(input, properties.getLength(), properties.getFillerCharacter());
            case RIGHT -> StringUtils.rightPad(input, properties.getLength(), properties.getFillerCharacter());
        };
    }
}
