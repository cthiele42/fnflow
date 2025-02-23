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

package org.ct42.fnflow.batchdlt;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Create a composed function bean composing all functional beans of type
 * - Function&lt;JsonNode, JsonNode&gt; and
 * - Function&lt;List&lt;BatchElement&gt;, List&lt;BatchElement&gt;&gt;
 *
 * @author Claas Thiele
 */
@AutoConfiguration
public class BatchDltConfiguration {
    @Bean
    ComposedFunction fnFlowComposedFnBean(ApplicationContext applicationContext) {
        return new ComposedFunction(applicationContext);
    }
}
