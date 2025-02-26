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

package org.ct42.fnflow.batchdlttest.mixedfnkafka;

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
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
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
        "spring.cloud.function.definition=" + MixedFnKafkaTest.COMPOSED_FN_NAME,
        "spring.cloud.stream.default.group=" + MixedFnKafkaTest.GROUP //,
})
public class MixedFnKafkaTest {
    public static final String GROUP = "testgroup";
    public static final String MULTIOUT_FN_NAME = "multiout";
    public static final String COMPOSED_FN_NAME = MULTIOUT_FN_NAME;
    public static final String IN_TOPIC = COMPOSED_FN_NAME.replace("|", "") + "-in-0";
    public static final String OUT_TOPIC = COMPOSED_FN_NAME.replace("|", "") + "-out-0";
    public static final String DLT_TOPIC = COMPOSED_FN_NAME.replace("|", "") + "-out-1";

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


    @Test
    public void testMixedFn() throws Exception {
        for (int i = 0; i < 10; i++) {
            template.sendDefault("T" + i);
        }
        List<ConsumerRecord<String, String>> results = new ArrayList<>();
        while (true) {
            ConsumerRecord<String, String> received =
                    inRecords.poll(200, TimeUnit.MILLISECONDS);
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
        then(results).extracting(ConsumerRecord::value).containsExactly(
                "MO: T0",
                "MO: T1",
                "MO: T2",
                "MO: T4",
                "MO: T5",
                "MO: T6",
                "MO: T8",
                "MO: T9"
        );
        then(errors).extracting(ConsumerRecord::value).containsExactly(
                "ERR: T3",
                "ERR: T7"
        );
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TestConfiguration {
    }

    @Component(MixedFnKafkaTest.MULTIOUT_FN_NAME)
    protected final static class MultiOut implements Function<Flux<String>, Tuple2<Flux<String>, Flux<String>>> {
        @Override
        public Tuple2<Flux<String>, Flux<String>> apply(Flux<String> stringFlux) {
            Sinks.Many<String> error = Sinks.many().unicast().onBackpressureBuffer();

            Flux<String> regular = stringFlux.mapNotNull(s -> {
                if(s.contains("T3") || s.contains("T7")) throw new RuntimeException("Should go to DLT");
                return "MO: " + s;
            }).onErrorContinue((throwable, s) -> error.tryEmitNext("ERR: " + s));
            return Tuples.of(regular, error.asFlux());
        }
    }
}
