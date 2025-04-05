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

package org.ct42.fnflow.fnflow_projector;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Claas Thiele
 */
@Component("project")
@RequiredArgsConstructor
@Slf4j
public class Project implements Consumer<Flux<Message<JsonNode>>> {
    private final ProjectorProperties properties;
    private final OpenSearchClient client;

    @Override
    public void accept(Flux<Message<JsonNode>> messageFlux) {
        messageFlux.bufferTimeout(properties.getBatchSize(), Duration.ofMillis(properties.getBatchTimeoutMs()))
                .flatMapSequential(messages -> {
                    BulkRequest.Builder builder = new BulkRequest.Builder();
                    try {
                        builder.index(properties.getIndex());

                        List<BulkOperation> operations = messages.stream().map(m -> {
                            byte[] keyBytes = m.getHeaders().get(KafkaHeaders.RECEIVED_KEY, byte[].class);
                            if(keyBytes != null) {
                                return new BulkOperation.Builder().index(
                                        new IndexOperation.Builder<JsonNode>()
                                                .index(properties.getIndex())
                                                .id(new String(keyBytes))
                                                .document(m.getPayload())
                                                .build()).build();
                            } else {
                                log.error("Missing key for message");
                                return null;
                            }
                        }).filter(Objects::nonNull).toList();

                        builder.operations(operations);
                        BulkResponse bulkResponse = client.bulk(builder.build());
                        if(bulkResponse.errors()) {
                            bulkResponse.items().forEach(i -> {
                                ErrorCause error = i.error();
                                if( error != null ) {
                                    log.error("Error while writing change events for event with id: {} error: {}", i.id(), error.reason());
                                }
                            });
                        }
                    } catch (IOException e) {
                        log.error("Error while bulk writing", e);
                    }
                    return Flux.empty();
                }).onErrorContinue((t, o) -> log.error("Error in processing change events", t)).subscribe();
    }
}
