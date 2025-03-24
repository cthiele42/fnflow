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

package org.ct42.fnflow.batchfnlib.emit;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.ct42.fnflow.batchdlt.HeaderAware;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.opensearch.common.UUIDs;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Claas Thiele
 */
@Component("ChangeEventEmit")
@RegisterReflection(classes = JsonPointer.class, memberCategories = MemberCategory.INVOKE_PUBLIC_METHODS)
public class ChangeEventEmit extends ConfigurableFunction<JsonNode, JsonNode, EmitProperties> implements HeaderAware {
    @Override
    public JsonNode apply(JsonNode input) {
        JsonNode contentNode = input.at(properties.getEventContent());
        if(contentNode.isObject()) {
            return contentNode;
        }
        return null;
    }

    @Override
    public Map<String, String> headersToBeAdded(JsonNode input) {
        Map<String, String> headers = new HashMap<>();

        JsonPointer keyPointer = properties.getEventKey();
        if(keyPointer != null) {
            JsonNode keyNode = input.at(keyPointer);
            if(!keyNode.isMissingNode() && !keyNode.isNull()) {
                if(keyNode.isTextual()) {
                    headers.put(KafkaHeaders.KEY, keyNode.asText());
                } else {
                    headers.put(KafkaHeaders.KEY, keyNode.toString());
                }
            }
        }
        if(!headers.containsKey(KafkaHeaders.KEY)) {
            headers.put(KafkaHeaders.KEY, UUIDs.base64UUID());
        }

        String topic = properties.getTopic();
        if(topic != null) {
            headers.put("spring.cloud.stream.sendto.destination", topic);
        }

        return headers;
    }
}
