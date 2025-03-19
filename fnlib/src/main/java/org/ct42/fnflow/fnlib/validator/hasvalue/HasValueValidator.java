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

package org.ct42.fnflow.fnlib.validator.hasvalue;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.ct42.fnflow.fnlib.validator.ValidationException;
import org.springframework.stereotype.Component;

/**
 * It is a function to check if an element in a JsonNode input object's specific path has a value.
 *
 * @author Sajjad Safaeian
 */
@Component("hasValueValidator")
public class HasValueValidator extends ConfigurableFunction<JsonNode, JsonNode, HasValueProperties> {

    @Override
    public JsonNode apply(JsonNode input) {
        JsonPointer pointer = properties.getElementPath();
        JsonNode resultNode = input.at(pointer);

        if(resultNode.isNull()) {
            throw new ValidationException("The value is null.");
        }

        if(resultNode.isMissingNode()) {
            throw new ValidationException("The element is not Exist.");
        }

        if(resultNode.isTextual() && StringUtils.isEmpty(resultNode.asText())) {
            throw new ValidationException("The value is empty text.");
        }

        if(resultNode.isArray()) {
            boolean hasValue = false;
            for (JsonNode node: resultNode) {
                if(node.isNumber() || (node.isTextual() && StringUtils.isNotEmpty(node.asText()))) {
                    hasValue = true;
                    break;
                }
            }

            if(!hasValue) {
                throw new ValidationException("The array element has not value.");
            }
        }

        if(resultNode.isObject()) {
            throw new ValidationException("Object type is not acceptable.");
        }

        return input;
    }
}