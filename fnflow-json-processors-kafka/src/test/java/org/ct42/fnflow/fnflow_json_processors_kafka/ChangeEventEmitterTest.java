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

package org.ct42.fnflow.fnflow_json_processors_kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@RegisterReflectionForBinding(classes = KafkaMessageListenerContainer.class)
@Testcontainers
@SpringBootTest(properties = {
        "cfgfns.ChangeEventEmit.input.eventContent=/input",
        "cfgfns.ChangeEventEmit.input.topic=source",
        "cfgfns.ChangeEventEmit.entity.eventContent=/matches/0/source",
        "cfgfns.ChangeEventEmit.entity.eventKey=/matches/0/id",
        "org.ct42.fnflow.function.definition=input+entity"
})
public class ChangeEventEmitterTest extends AbstractKafkaTest {
    public static final String IN_TOPIC = "fnFlowComposedFnBean-in-0";
    public static final String ENTITY_TOPIC = "fnFlowComposedFnBean-out-0";
    public static final String DLT_TOPIC = "fnFlowComposedFnBean-out-1";
    public static final String SOURCE_TOPIC = "source";

    @DynamicPropertySource
    static void brokerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafkaContainer::getBootstrapServers);
    }

    private final BlockingQueue<ConsumerRecord<byte[], String>> inRecords = new LinkedBlockingQueue<>();
    private final BlockingQueue<ConsumerRecord<byte[], String>> errRecords = new LinkedBlockingQueue<>();
    private final BlockingQueue<ConsumerRecord<byte[], String>> srcRecords = new LinkedBlockingQueue<>();

    @BeforeEach
    void setup() throws Exception {
        setupProducer(IN_TOPIC);
        setupConsumer(inRecords, ENTITY_TOPIC);
        setupConsumer(errRecords, DLT_TOPIC);
        setupConsumer(srcRecords, SOURCE_TOPIC);
    }

    @Test
    void testEmitter() throws Exception{
        template.sendDefault("""
                {
                    "input": {"foo": "bar"},
                    "matches": [{"source": {"text": "Result"}}]
                }""");
        template.sendDefault("""
                {
                    "input": null,
                    "matches": []
                }""");
        template.sendDefault("""
                {
                    "input": {"foo": "bar2"},
                    "matches": [{"id": "givenID", "source": {"text": "Result with ID"}}]
                }""");

        List<ConsumerRecord<byte[], String>> entityEvents = getConsumerRecords(inRecords, 2000);
        List<ConsumerRecord<byte[], String>> sourceEvents = getConsumerRecords(srcRecords, 2000);
        List<ConsumerRecord<byte[], String>> errors = getConsumerRecords(errRecords, 200);

        then(errors).isEmpty();

        then(entityEvents).hasSize(2);
        then(new String(entityEvents.get(0).key())).hasSize(20);
        then(entityEvents.get(0).value()).isEqualTo("""
                {"text":"Result"}""");
        then(new String(entityEvents.get(1).key())).isEqualTo("givenID");
        then(entityEvents.get(1).value()).isEqualTo("""
                {"text":"Result with ID"}""");

        then(sourceEvents).hasSize(2);
        then(new String(sourceEvents.get(0).key())).hasSize(20);
        then(sourceEvents.get(0).value()).isEqualTo("""
                {"foo":"bar"}""");
        then(new String(sourceEvents.get(1).key())).hasSize(20);
        then(sourceEvents.get(1).value()).isEqualTo("""
                {"foo":"bar2"}""");
    }

}
