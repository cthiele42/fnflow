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

package org.ct42.fnflow.batchdlttest.headeraware;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.ct42.fnflow.batchdlt.BatchElement;
import org.ct42.fnflow.batchdlt.HeaderAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Claas Thiele
 */
@SpringBootTest
@EmbeddedKafka(bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers", partitions = 1)
@TestPropertySource(properties = {
        "spring.cloud.function.definition=fnFlowComposedFnBean",
        "spring.cloud.stream.default.group=test",
        "org.ct42.fnflow.function.definition=single|batch|outA+outB"
})
public class HeaderAwareTest {
    public static final String IN_TOPIC = "fnFlowComposedFnBean-in-0";
    public static final String OUT_TOPIC = "fnFlowComposedFnBean-out-0";
    public static final String DLT_TOPIC = "fnFlowComposedFnBean-out-1";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private KafkaTemplate<String, String> template;
    private final BlockingQueue<ConsumerRecord<String, String>> inRecords = new LinkedBlockingQueue<>();
    private final BlockingQueue<ConsumerRecord<String, String>> errRecords = new LinkedBlockingQueue<>();

    @BeforeEach
    public void setUp() {
        setupProducer();
        setupConsumer(inRecords, OUT_TOPIC);
        setupConsumer(errRecords, DLT_TOPIC);
    }

    @Test
    void testMultiplyingMessages() throws Exception {
        for (int i = 0; i < 3; i++) {
            template.sendDefault("{\"text\":\"T" + i + "\"}");
        }
        List<ConsumerRecord<String, String>> results = new ArrayList<>();
        while (true) {
            ConsumerRecord<String, String> received =
                    inRecords.poll(2000, TimeUnit.MILLISECONDS);
            if (received == null) {
                break;
            }
            results.add(received);
        }
        List<ConsumerRecord<String, String>> errors = new ArrayList<>();
        while (true) {
            ConsumerRecord<String, String> received =
                    errRecords.poll(200, TimeUnit.MILLISECONDS);
            if (received == null) {
                break;
            }
            errors.add(received);
        }
        then(errors).isEmpty();
        then(results).hasSize(6);

        Header[] headersA = new Header[]{
                new RecordHeader("logSingle", "seen by single function".getBytes(StandardCharsets.UTF_8)),
                new RecordHeader("logMultiA", "seen by multiout function A".getBytes(StandardCharsets.UTF_8)),
                new RecordHeader("logBatch", "seen by batch function".getBytes(StandardCharsets.UTF_8))
        };
        Header[] headersB = new Header[]{
                new RecordHeader("logSingle", "seen by single function".getBytes(StandardCharsets.UTF_8)),
                new RecordHeader("logMultiB", "seen by multiout function B".getBytes(StandardCharsets.UTF_8)),
                new RecordHeader("logBatch", "seen by batch function".getBytes(StandardCharsets.UTF_8))
        };
        then(results.get(0).headers().toArray()).contains(headersA);
        then(results.get(1).headers().toArray()).contains(headersB);
        then(results.get(2).headers().toArray()).contains(headersA);
        then(results.get(3).headers().toArray()).contains(headersB);
        then(results.get(4).headers().toArray()).contains(headersA);
        then(results.get(5).headers().toArray()).contains(headersB);
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TestConfiguration {
    }

    @Component("single")
    protected final static class Single implements Function<JsonNode, JsonNode>, HeaderAware {
        @Override
        public JsonNode apply(JsonNode n) {
            ((ObjectNode)n).put("logSingle", "seen by single function");
            return n;
        }

        @Override
        public Map<String, String> headersToBeAdded(JsonNode output) {
            return Map.of("logSingle", "seen by single function");
        }
    }

    @Component("outA")
    protected final static class OutA implements Function<JsonNode, JsonNode>, HeaderAware {
        @Override
        public JsonNode apply(JsonNode n) {
            ((ObjectNode)n).put("out", "A");
            return n;
        }

        @Override
        public Map<String, String> headersToBeAdded(JsonNode output) {
            return Map.of("logMultiA", "seen by multiout function A");
        }
    }

    @Component("outB")
    protected final static class OutB implements Function<JsonNode, JsonNode>, HeaderAware {
        @Override
        public JsonNode apply(JsonNode n) {
            ((ObjectNode)n).put("out", "B");
             return n;
        }

        @Override
        public Map<String, String> headersToBeAdded(JsonNode output) {
            return Map.of("logMultiB", "seen by multiout function B");
        }
    }

    @Component("batch")
    protected final static class Batch implements Function<List<BatchElement>, List<BatchElement>>, HeaderAware {
        /**
         * @param b the batch; will be not empty
         * @return the batch with processed values
         */
        @Override
        public List<BatchElement> apply(List<BatchElement> b) {
            b.forEach(e -> {
                ((ObjectNode)e.getInput()).put("logBatch", "seen by batch function");
                e.processWithOutput(e.getInput());
            });
            return b;
        }

        @Override
        public Map<String, String> headersToBeAdded(JsonNode output) {
            return Map.of("logBatch", "seen by batch function");
        }
    }

    private void setupConsumer(BlockingQueue<ConsumerRecord<String, String>> queue, String topic) {
        // set up the Kafka consumer properties
        Map<String, Object> consumerProperties =
                KafkaTestUtils.consumerProps("sender", "false", embeddedKafka);

        // create a Kafka consumer factory
        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProperties);

        // set the topic that needs to be consumed
        ContainerProperties containerProperties =
                new ContainerProperties(topic);

        // create a Kafka MessageListenerContainer
        KafkaMessageListenerContainer<String, String> inContainer =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        // setup a Kafka message listener
        inContainer.setupMessageListener((MessageListener<String, String>) queue::add);

        // start the container and underlying message listener
        inContainer.start();

        // wait until the container has the required number of assigned partitions
        ContainerTestUtils.waitForAssignment(inContainer, 1);
    }

    private void setupProducer() {
        // set up the Kafka producer properties
        Map<String, Object> senderProperties =
                KafkaTestUtils.producerProps(
                        embeddedKafka.getBrokersAsString());

        // create a Kafka producer factory
        ProducerFactory<String, String> producerFactory =
                new DefaultKafkaProducerFactory<>(
                        senderProperties);

        // create a Kafka template
        template = new KafkaTemplate<>(producerFactory);
        // set the default topic to send to
        template.setDefaultTopic(IN_TOPIC);

        // wait until the partitions are assigned
        for (MessageListenerContainer messageListenerContainer : kafkaListenerEndpointRegistry
                .getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(messageListenerContainer,
                    embeddedKafka.getPartitionsPerTopic());
        }
    }
}
