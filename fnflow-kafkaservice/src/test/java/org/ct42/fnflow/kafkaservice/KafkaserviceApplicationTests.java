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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.util.MultiValueMap;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
properties = {
	"spring.kafka.producer.batch-size=131072",
	"spring.kafka.producer.compression-type=lz4",
	"spring.kafka.producer.properties.linger.ms=50"
})
class KafkaserviceApplicationTests {
	public static final String TOPIC = "testtopic";

	@Autowired
	private TestRestTemplate restTemplate;

	@Container
	@ServiceConnection
	static KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka-native:3.8.1");

	@Autowired
	private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

	private final BlockingQueue<ConsumerRecord<String, String>> inRecords = new LinkedBlockingQueue<>();

	@BeforeEach
	void setup() {
		setupConsumer(inRecords, TOPIC);
	}

	@Test
	void testWriteBatch() throws Exception {
		String content = """
				{
					"messages": [
						{
							"key": "key0",
							"headers": [{"key": "KEY","value": "key0"}],
							"value": {"id":"ID0"}
						},
						{
							"key": "key1",
							"headers": [{"key": "KEY","value": "key1"}],
							"value": {"id":"ID1"}
						},
						{
							"key": "key2",
							"headers": [{"key": "KEY","value": "key2"}],
							"value": {"id":"ID2"}
						}
					]
				}""";

		HttpEntity<String> httpEntity = new HttpEntity<>(content, MultiValueMap.fromSingleValue(Map.of("Content-Type", "application/json")));
		ResponseEntity<Void> response = restTemplate.postForEntity("/" + TOPIC, httpEntity, Void.class);

		then(response.getStatusCode().is2xxSuccessful()).isTrue();

		List<ConsumerRecord<String, String>> results = new ArrayList<>();
		while (true) {
			ConsumerRecord<String, String> received =
					inRecords.poll(2000, TimeUnit.MILLISECONDS);
			if (received == null) {
				break;
			}
			results.add(received);
		}

		then(results).hasSize(3);
		then(results.getFirst().key()).isEqualTo("key0");
		then(results.getFirst().value()).isEqualTo("""
				{"id":"ID0"}""");
		then(results.getFirst().headers()).hasSize(1);
		then(new String(results.getFirst().headers().lastHeader("KEY").value())).isEqualTo("key0");
	}

	private void setupConsumer(BlockingQueue<ConsumerRecord<String, String>> queue, String topic) {
		// set up the Kafka consumer properties
		Map<String, Object> consumerProperties =
				KafkaTestUtils.consumerProps(kafkaContainer.getBootstrapServers(), "sender");
		consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
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
}
