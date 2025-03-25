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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Wrapper function for functions of type Function&lt;List&lt;BatchElement&gt;, List&lt;BatchElement&gt;&gt;.
 * Organizes a buffer to pass a batch to the target function.
 * Is mapping the input from Message and the result back to Message.
 *
 * @author Claas Thiele
 */
public class BatchFnWrapper implements BiFunction<Flux<Message<JsonNode>>, Sinks.Many<Message<Throwable>>, Flux<Message<JsonNode>>> {
        public final int defaultBatchSize;
        public final Duration defaultBatchTimeout;

        private final Function<List<BatchElement>, List<BatchElement>> target;

        public BatchFnWrapper(Function<List<BatchElement>, List<BatchElement>> target, int defaultBatchSize, long defaultBatchTimeout) {
            this.defaultBatchSize = defaultBatchSize;
            this.defaultBatchTimeout = Duration.ofMillis(defaultBatchTimeout);
            this.target = target;
        }

        @Override
        public Flux<Message<JsonNode>> apply(Flux<Message<JsonNode>> messageFlux, Sinks.Many<Message<Throwable>> error) {
            return messageFlux.bufferTimeout(defaultBatchSize, defaultBatchTimeout).flatMapSequential(b -> {
                List<BatchElement> results = target.apply(b.stream().map(e -> new BatchElement(e.getPayload())).toList());
                List<Message<JsonNode>> resultMsgs = new ArrayList<>();
                for(int i = 0; i < results.size(); i++) {
                    BatchElement result = results.get(i);
                    if(result.getOutput() != null) {
                        MessageBuilder builder = MessageBuilder
                            .withPayload(result.getOutput())
                            .copyHeaders(b.get(i).getHeaders());
                        if(target instanceof HeaderAware headerAware) {
                            headerAware.headersToBeAdded(result.getInput())
                                .forEach(builder::setHeader);
                        }
                        Message<JsonNode> msg = builder.build();
                        resultMsgs.add(msg);
                    } else if (result.getError() != null) {
                        error.tryEmitNext(MessageBuilder
                                .withPayload(result.getError())
                                .copyHeaders(b.get(i).getHeaders())
                                .build());
                    }
                }
                return Flux.fromIterable(resultMsgs);
            });
        }
    }
