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
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Sajjad Safaeian
 */
public abstract class AbstractKafkaTest {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka-native:3.8.1");

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    protected KafkaTemplate<String, String> template;

    protected @NotNull List<ConsumerRecord<byte[], String>> getConsumerRecords(BlockingQueue<ConsumerRecord<byte[], String>> outputRecords, int timeout) throws InterruptedException {
        List<ConsumerRecord<byte[], String>> outputEvents = new ArrayList<>();
        while (true) {
            ConsumerRecord<byte[], String> received =
                    outputRecords.poll(timeout, TimeUnit.MILLISECONDS);
            if (received == null) {
                break;
            }
            outputEvents.add(received);
        }
        return outputEvents;
    }

    protected void setupProducer(String topic) {
        // set up the Kafka producer properties
        Map<String, Object> senderProperties = KafkaTestUtils.producerProps(kafkaContainer.getBootstrapServers());

        // create a Kafka producer factory
        ProducerFactory<String, String> producerFactory =
                new DefaultKafkaProducerFactory<>(
                        senderProperties);

        // create a Kafka template
        template = new KafkaTemplate<>(producerFactory);
        // set the default topic to send to
        template.setDefaultTopic(topic);

        // wait until the partitions are assigned
        for (MessageListenerContainer messageListenerContainer : kafkaListenerEndpointRegistry
                .getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(messageListenerContainer, 1);
        }
    }

    protected void setupConsumer(BlockingQueue<ConsumerRecord<byte[], String>> queue, String topic) {
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


}
