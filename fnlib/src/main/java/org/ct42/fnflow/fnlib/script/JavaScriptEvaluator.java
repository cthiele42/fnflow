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
import org.graalvm.polyglot.Context;
import org.springframework.stereotype.Component;

/**
 * @author Sajjad Safaeian
 */
@Component
public class JavaScriptEvaluator extends AbstractScriptEvaluator {

    @Override
    public JsonNode evaluate(String script, JsonNode input) {
        try (Context context = Context.create()) {
            String language = ScriptProperties.ScriptLanguage.JS.getName();

            return evaluate(script, input, language, context);
        }
    }
}
