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

package org.ct42.fnflow.batchdlttest.multiply;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
 * @author Sajjad Safaeian
 */
@SpringBootTest
@EmbeddedKafka(bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers", partitions = 1)
@TestPropertySource(properties = {
        "spring.cloud.function.definition=fnFlowComposedFnBean",
        "spring.cloud.stream.default.group=test",
        "org.ct42.fnflow.function.definition=outA+outB"
})
public class MultiplyTest {
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
        for (int i = 0; i < 10; i++) {
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
        then(errors).hasSize(1);
        then(errors.getFirst().value()).isEqualTo("{\"text\":\"T0\"}");
        then(results).hasSize(17);
        then(results.getFirst().value()).isEqualTo("{\"text\":\"T0\",\"out\":\"A\"}");
        then(results.get(1).value()).isEqualTo("{\"text\":\"T1\",\"out\":\"B\"}");
        then(results.get(2).value()).isEqualTo("{\"text\":\"T2\",\"out\":\"A\"}");
        then(results.get(14).value()).isEqualTo("{\"text\":\"T8\",\"out\":\"A\"}");
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TestConfiguration {
    }

    @Component("outA")
    protected final static class OutA implements Function<JsonNode, JsonNode> {
        @Override
        public JsonNode apply(JsonNode n) {
            if(n.toString().contains("T1")) return null;
            ((ObjectNode)n).put("out", "A");
            return n;
        }
    }

    @Component("outB")
    protected final static class OutB implements Function<JsonNode, JsonNode> {
        @Override
        public JsonNode apply(JsonNode n) {
            if(n.toString().contains("T8")) return null;

            if(n.toString().contains("T0"))
                throw new RuntimeException("Sample Exception");

            ((ObjectNode)n).put("out", "B");
             return n;
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
