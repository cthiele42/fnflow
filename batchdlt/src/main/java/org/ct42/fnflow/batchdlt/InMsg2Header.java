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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 *
 * @author Claas Thiele
 */
public class InMsg2Header implements Function<Flux<Message<byte[]>>, Flux<Message<JsonNode>>> {
    public static final String IN_PAYLOAD_HEADER = "__IN_PAYLOAD_BYTEARRAY";
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Flux<Message<JsonNode>> apply(Flux<Message<byte[]>> messageFlux) {
        return messageFlux.handle((m, sink) -> sink.next(MessageBuilder
                .withPayload(mapper.readValue(m.getPayload(), JsonNode.class))
                .copyHeaders(m.getHeaders())
                .setHeader(IN_PAYLOAD_HEADER, m.getPayload())
                .build()));
    }
}
