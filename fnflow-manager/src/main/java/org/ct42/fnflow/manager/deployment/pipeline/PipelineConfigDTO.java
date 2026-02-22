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

package org.ct42.fnflow.manager.deployment.pipeline;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ct42.fnflow.manager.deployment.AbstractConfigDTO;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tools.jackson.databind.type.TypeFactory.createDefaultInstance;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PipelineConfigDTO extends AbstractConfigDTO {
    private String sourceTopic;
    private String entityTopic;
    private String errorTopic;
    private CleanUpMode cleanUpMode = CleanUpMode.COMPACT;
    private int cleanUpTimeHours = 336; //default 14 days
    private int errRetentionHours = 336; //default 14 days
    private List<Function> pipeline;

    @Data
    public static class FunctionCfg {
        private String name;
        private String function;
        private Map<String, Object> parameters = new HashMap<>();
    }

    @JsonDeserialize(using = FunctionDeserializer.class)
    @JsonSerialize(using = FunctionSerializer.class)
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

    public static class FunctionDeserializer extends ValueDeserializer<Function> {
        @Override
        public Function deserialize(JsonParser p, DeserializationContext ctxt) {
            if (p.currentToken() == JsonToken.START_OBJECT) {
                FunctionCfg function = p.readValueAs(FunctionCfg.class);
                return new SingleFunction(function);
            } else if (p.currentToken() == JsonToken.START_ARRAY) {
                List<FunctionCfg> functions =
                        ctxt.readValue(p, createDefaultInstance().constructCollectionType(List.class, FunctionCfg.class));

                return new MultipleFunctions(functions);
            }

            return null;
        }
    }

    public static class FunctionSerializer extends ValueSerializer<Function> {
        @Override
        public void serialize(Function value, JsonGenerator gen, SerializationContext serializers) {
            switch (value) {
                case SingleFunction singleFunction ->
                        gen.writePOJO(singleFunction.getFunction());
                case MultipleFunctions multipleFunctions ->
                        gen.writePOJO(multipleFunctions.getFunctions());
                default ->
                        gen.writeNull();
            }
        }
    }

    public enum CleanUpMode {
        COMPACT, DELETE
    }
}
