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

import org.ct42.fnflow.functions.ConfigurableFunction;
import org.springframework.stereotype.Component;

/**
 * @author Claas Thiele
 */
@Component
public class Replace extends ConfigurableFunction<String, String, ReplaceProperties> {
    @Override
    public String apply(String input) {
        return input.replace(properties.getPattern(), properties.getReplace());
    }
}
