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

package org.ct42.fnflow.manager;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Claas Thiele
 */
@Data
public class PipelineConfigDTO {
    private String version;
    private String sourceTopic;
    private String entityTopic;
    private int errRetentionHours = 336; //default 14 days
    private int outCompactionLagHours = 744; //default 31 days
    private FunctionCfg[] pipeline;

    @Data
    public static class FunctionCfg {
        private String name;
        private String function;
        private Map<String, Object> parameters = new HashMap<>();
    }
}
