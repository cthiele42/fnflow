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

package org.ct42.fnflow.fnlib.script;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * @author Sajjad Safaeian
 */
public abstract class AbstractScriptEvaluator implements ScriptEvaluator {

    private final ObjectMapper mapper = new ObjectMapper();

    protected JsonNode evaluate(String script, JsonNode input, String language, Context context) {
        try {
            context.getBindings(language).putMember("input", input.toString());

            Value evaluationResult = context.eval(language, script);

            Object raw = evaluationResult.as(Object.class);
            String json = mapper.writeValueAsString(raw);
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
