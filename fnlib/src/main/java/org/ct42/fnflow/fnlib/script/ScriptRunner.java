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
import lombok.RequiredArgsConstructor;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.springframework.stereotype.Component;

/**
 * @author Sajjad Safaeian
 */
@Component("ScriptRunner")
@RequiredArgsConstructor
public class ScriptRunner extends ConfigurableFunction<JsonNode, JsonNode, ScriptProperties>  {

    private final ScriptEvaluatorFactory scriptEvaluatorFactory;

    @Override
    public JsonNode apply(JsonNode input) {
        try {
            ScriptEvaluator evaluator = scriptEvaluatorFactory.createScriptEvaluator(properties.getScriptLanguage());

            return evaluator.evaluate(properties.getScript(), input);
        } catch (Exception e) {
            throw new ScriptRunnerException("Script: " + properties.getScript(), e);
        }
    }

}
