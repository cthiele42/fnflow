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

package org.ct42.fnflow.fnlib.merger;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.ct42.fnflow.fnlib.utils.TreeByPointerBuilder;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@Component("MergeCreate")
@RegisterReflectionForBinding(classes = {MergeProperties.class, MergeProperties.Mapping.class})
public class MergeCreate extends ConfigurableFunction<JsonNode, JsonNode, MergeProperties> {
    @Override
    public JsonNode apply(JsonNode input) {
        JsonNode inputObj = input.get("input");
        if (!inputObj.isNull() && input.get("matches").isArray() && !input.get("matches").isEmpty()) {
            properties.getMappings().forEach(m -> {
                JsonNode from = inputObj.at(m.getFrom());
                JsonNode to = input.at("/matches/0/source");
                if(!from.isMissingNode() && to.isObject() && to.at(m.getTo()).isMissingNode()) {
                    TreeByPointerBuilder.buildAndSet(m.getTo(), (ObjectNode)to, from);
                }
            });
        }
        return input;
    }
}
