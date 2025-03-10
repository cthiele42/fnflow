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

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;

/**
 *
 * @author Claas Thiele
 */
public class ErrorConvert2ByteArray implements Function<Flux<Message<Throwable>>, Flux<Message<byte[]>>> {
    @Override
    public Flux<Message<byte[]>> apply(Flux<Message<Throwable>> f) {
        return f.map(m -> {
            MessageHeaders headers = m.getHeaders();
            byte[] inPayload = (byte[])headers.get(InMsg2Header.IN_PAYLOAD_HEADER);

            return MessageBuilder
                    .withPayload(inPayload)
                    .copyHeaders(headers)
                    .setHeader("x-exception-message", m.getPayload().getMessage())
                    .setHeader("x-exception-fqcn", m.getPayload().getClass().getName())
                    .removeHeader(InMsg2Header.IN_PAYLOAD_HEADER)
                    .build();
        });
    }
}
