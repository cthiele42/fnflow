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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Claas Thiele
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
public class ChangeEventEmitterTest {
    public static final String IN_TOPIC = "fnFlowComposedFnBean-in-0";
    public static final String ENTITY_TOPIC = "fnFlowComposedFnBean-out-0";
    public static final String DLT_TOPIC = "fnFlowComposedFnBean-out-1";
    public static final String SOURCE_TOPIC = "source";

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka-native:3.8.1");

    @DynamicPropertySource
    static void brokerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private KafkaTemplate<String, String> template;
    private final BlockingQueue<ConsumerRecord<byte[], String>> inRecords = new LinkedBlockingQueue<>();
    private final BlockingQueue<ConsumerRecord<byte[], String>> errRecords = new LinkedBlockingQueue<>();
    private final BlockingQueue<ConsumerRecord<byte[], String>> srcRecords = new LinkedBlockingQueue<>();

    @BeforeEach
    void setup() throws Exception {
        setupProducer();
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

        List<ConsumerRecord<byte[], String>> entityEvents = new ArrayList<>();
        while (true) {
            ConsumerRecord<byte[], String> received =
                    inRecords.poll(2000, TimeUnit.MILLISECONDS);
            if (received == null) {
                break;
            }
            entityEvents.add(received);
        }
        List<ConsumerRecord<byte[], String>> sourceEvents = new ArrayList<>();
        while (true) {
            ConsumerRecord<byte[], String> received =
                    srcRecords.poll(200, TimeUnit.MILLISECONDS);
            if (received == null) {
                break;
            }
            sourceEvents.add(received);
        }
        List<ConsumerRecord<byte[], String>> errors = new ArrayList<>();
        while (true) {
            ConsumerRecord<byte[], String> received =
                    errRecords.poll(200, TimeUnit.MILLISECONDS);
            if (received == null) {
                break;
            }
            errors.add(received);
        }

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

    private void setupConsumer(BlockingQueue<ConsumerRecord<byte[], String>> queue, String topic) {
        // set up the Kafka consumer properties
        Map<String, Object> consumerProperties =
                KafkaTestUtils.consumerProps(kafkaContainer.getBootstrapServers(), "sender");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        // create a Kafka consumer factory
        DefaultKafkaConsumerFactory<byte[], String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProperties);

        // set the topic that needs to be consumed
        ContainerProperties containerProperties =
                new ContainerProperties(topic);

        // create a Kafka MessageListenerContainer
        KafkaMessageListenerContainer<byte[], String> inContainer =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        // setup a Kafka message listener
        inContainer.setupMessageListener((MessageListener<byte[], String>) queue::add);

        // start the container and underlying message listener
        inContainer.start();

        // wait until the container has the required number of assigned partitions
        ContainerTestUtils.waitForAssignment(inContainer, 1);
    }

    private void setupProducer() {
        // set up the Kafka producer properties
        Map<String, Object> senderProperties = KafkaTestUtils.producerProps(kafkaContainer.getBootstrapServers());

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
            ContainerTestUtils.waitForAssignment(messageListenerContainer, 1);
        }
    }
}
