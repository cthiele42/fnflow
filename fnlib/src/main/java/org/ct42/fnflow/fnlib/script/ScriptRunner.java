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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

/**
 * @author Sajjad Safaeian
 */
@Component("ScriptRunner")
public class ScriptRunner extends ConfigurableFunction<JsonNode, JsonNode, ScriptProperties>  {

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public JsonNode apply(JsonNode input) {
        try (Context context = Context.create()) {
            try {
                context.getBindings("js").putMember("input", input.toString());
                Value evaluationResult = context.eval(properties.getScript());

                Object raw = evaluationResult.as(Object.class);
                String json = mapper.writeValueAsString(raw);
                return mapper.readTree(json);

            } catch (Exception e) {
                throw new ScriptRunnerException("Script: " + properties.getScript().getCharacters().toString(), e);
            }
        }
    }

}
