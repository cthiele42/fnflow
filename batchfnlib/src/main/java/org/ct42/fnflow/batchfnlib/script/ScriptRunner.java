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

package org.ct42.fnflow.batchfnlib.script;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ct42.fnflow.batchdlt.BatchElement;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sajjad Safaeian
 */
@Component("ScriptRunner")
@RegisterReflection(classes = JsonPointer.class, memberCategories = MemberCategory.INVOKE_PUBLIC_METHODS)
public class ScriptRunner extends ConfigurableFunction<List<BatchElement>, List<BatchElement>, ScriptProperties> {
    @Override
    public List<BatchElement> apply(List<BatchElement> input) {
        try (Context context = Context.create()) {

            List<BatchElement> result = new ArrayList<>();

            AtomicInteger index = new AtomicInteger();
            input.forEach(batchElement -> {
                try {
                    int currentInput = index.getAndIncrement();
                    context.getBindings("js").putMember("input", batchElement.getInput().toString());
                    Value evaluationResult = context.eval(properties.getScript());

                    ObjectMapper mapper = new ObjectMapper();
                    List<JsonNode> evaluatedJsons = new ArrayList<>();
                    for (int i = 0; i < evaluationResult.getArraySize(); i++) {
                        Object raw = evaluationResult.getArrayElement(i).as(Object.class);
                        String json = mapper.writeValueAsString(raw);
                        evaluatedJsons.add(mapper.readTree(json));
                    }

                    result.addAll(
                            evaluatedJsons
                                .stream()
                                .map(jsonNode -> {
                                    BatchElement element = new BatchElement(batchElement.getInput(), currentInput);
                                    element.processWithOutput(jsonNode);
                                    return element;
                                })
                                .toList()
                    );

                } catch (Exception e) {
                    batchElement.processWithError(e);
                    result.add(batchElement);
                }
            });

            return result;
        }
    }
}
