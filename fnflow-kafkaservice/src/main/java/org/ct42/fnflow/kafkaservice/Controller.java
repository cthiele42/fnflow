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

package org.ct42.fnflow.kafkaservice;

import lombok.RequiredArgsConstructor;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Claas Thiele
 */
@RestController
@RequiredArgsConstructor
@RegisterReflectionForBinding(classes = {Message.class, Header.class, ReadMessage.class})
public class Controller {
    private final KafkaService kafkaService;

    @PostMapping(value="/{topic}")
    public ResponseEntity<Void> write(@PathVariable String topic,
                      @RequestBody BatchDTO batch) {
        kafkaService.write(topic, null, batch.getMessages());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value="/{topic}/{partition}")
    public ResponseEntity<Void> write(@PathVariable String topic,
                                @PathVariable int partition,
                                @RequestBody BatchDTO batch) {
        kafkaService.write(topic, partition, batch.getMessages());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{topic}")
    public TopicInfoDTO getTopicInfo(@PathVariable String topic) throws TopicDoesNotExistException {
        return kafkaService.getTopicInfo(topic);
    }

    @DeleteMapping("/{topic}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable String topic) throws TopicDoesNotExistException {
        kafkaService.deleteTopic(topic);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{topic}/{partition}")
    public ReadBatchDTO read(@PathVariable String topic, @PathVariable int partition) throws TopicDoesNotExistException {
        return kafkaService.read(topic, partition, null, null);
    }

    @GetMapping("/{topic}/{partition}/{from}")
    public ReadBatchDTO read(@PathVariable String topic, @PathVariable int partition, @PathVariable String from) throws TopicDoesNotExistException {
        return kafkaService.read(topic, partition, from, null);
    }

    @GetMapping("/{topic}/{partition}/{from}/{to}")
    public ReadBatchDTO read(@PathVariable String topic, @PathVariable int partition, @PathVariable String from, @PathVariable String to) throws TopicDoesNotExistException {
        return kafkaService.read(topic, partition, from, to);
    }
}
