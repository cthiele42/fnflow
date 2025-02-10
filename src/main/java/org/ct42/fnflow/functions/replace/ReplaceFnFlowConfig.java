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

package org.ct42.fnflow.functions.replace;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.ct42.fnflow.functions.ConfigurableFunction;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * @author Claas Thiele
 */
@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "functions.replace")
@RegisterReflectionForBinding(ReplaceProperties.class)
public class ReplaceFnFlowConfig extends HashMap<String, ReplaceProperties> {
    private final FunctionRegistry functionRegistry;
    private final ReplaceConfig replaceConfig;

    @PostConstruct
    private void registerFn() {
        for (Entry<String, ReplaceProperties> e : entrySet()) {
            ConfigurableFunction<String, String, ReplaceProperties> function =
                    replaceConfig.replaceFunction(e.getValue());
            functionRegistry.register(new FunctionRegistration<>(function, e.getKey()).type(Replace.class));

        }
    }
}
