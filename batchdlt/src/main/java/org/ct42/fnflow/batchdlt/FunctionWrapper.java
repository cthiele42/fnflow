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

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Wrapper function for functions of type Function&gt;JsonNode, JsonNode&gt;.
 *
 * @author Claas Thiele
 */
public class FunctionWrapper implements BiFunction<Flux<Message<JsonNode>>, Sinks.Many<Message<Throwable>>, Flux<Message<JsonNode>>> {
    private final Function<JsonNode, JsonNode> target;

    public FunctionWrapper(Function<JsonNode, JsonNode> target) {
        this.target = target;
    }

    @Override
    public Flux<Message<JsonNode>> apply(Flux<Message<JsonNode>> messageFlux, Sinks.Many<Message<Throwable>> error) {
        return messageFlux.mapNotNull(m -> {
            Map<String, Object> headersToBeAdded = new HashMap<>(0);
            if(target instanceof HeaderAware headerAware) {
                headersToBeAdded = headerAware.headersToBeAdded(m.getPayload());
            }
            JsonNode result = target.apply(m.getPayload());
            if(result == null) return null; // if the function is resulting to null, message is discarded
            MessageBuilder<JsonNode> builder = MessageBuilder.withPayload(result)
                    .copyHeaders(m.getHeaders());
            headersToBeAdded.forEach(builder::setHeader);
            return builder.build();
        }).onErrorContinue((throwable, m) -> error.tryEmitNext(
                            MessageBuilder
                                    .withPayload(throwable)
                                    .copyHeaders(((Message<JsonNode>)m).getHeaders())
                                    .build()
                        ));
    }
}
