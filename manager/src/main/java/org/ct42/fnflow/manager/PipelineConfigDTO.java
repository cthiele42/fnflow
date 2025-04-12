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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@Data
public class PipelineConfigDTO {
    private String version;
    private String sourceTopic;
    private String entityTopic;
    private String errorTopic;
    private int errRetentionHours = 336; //default 14 days
    private int outCompactionLagHours = 744; //default 31 days
    private List<Function> pipeline;

    @Data
    public static class FunctionCfg {
        private String name;
        private String function;
        private Map<String, Object> parameters = new HashMap<>();
    }

    @JsonDeserialize(using = FunctionDeserializer.class)
    public interface Function {}

    @Data
    @AllArgsConstructor
    public static class SingleFunction implements Function {
        private FunctionCfg function;
    }

    @Data
    @AllArgsConstructor
    public static class MultipleFunctions implements Function {
        private List<FunctionCfg> functions;
    }

    public static class FunctionDeserializer extends JsonDeserializer<Function> {
        @Override
        public Function deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.getCurrentToken() == JsonToken.START_OBJECT) {
                FunctionCfg function = p.readValueAs(FunctionCfg.class);
                return new SingleFunction(function);
            } else if (p.getCurrentToken() == JsonToken.START_ARRAY) {
                List<FunctionCfg> functions =
                        ctxt.readValue(p, defaultInstance().constructCollectionType(List.class, FunctionCfg.class));

                return new MultipleFunctions(functions);
            }

            return null;
        }
    }

}
