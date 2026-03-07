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

import tools.jackson.databind.JsonNode;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Claas Thiele
 */
@Testcontainers
@SpringBootTest(properties = {
		"fnflow.projector.index=testindex"
})
class FnflowProjectorApplicationTests {
	@Container
	static final OpenSearchContainer<?> container = new OpenSearchContainer<>("opensearchproject/opensearch:3.5.0");

	@Container
	static KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka-native:3.8.1");

	@Autowired
	private OpenSearchClient client;

	@DynamicPropertySource
	static void opensearchProperties(DynamicPropertyRegistry registry) {
		registry.add("opensearch.uris", container::getHttpHostAddress);
		registry.add("spring.cloud.stream.kafka.binder.brokers", kafkaContainer::getBootstrapServers);
	}

	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
	private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

	private KafkaTemplate<String, String> template;

	@BeforeEach
	void setup() {
		setupProducer();
	}

	@Test
	void testProjection() throws Exception {
		for (int i = 0; i < 510; i++) {
			template.sendDefault("key" + i, " {\"foo\":\"bar" + i + "\"}");
		}

		await().untilAsserted(() -> assertThat(countOnIndex()).isEqualTo(510));

		then(client.get(new GetRequest.Builder().index("testindex").id("key0").build(), JsonNode.class).source().toString())
				.isEqualTo("""
								{"foo":"bar0"}""");
		then(client.get(new GetRequest.Builder().index("testindex").id("key509").build(), JsonNode.class).source().toString())
				.isEqualTo("""
								{"foo":"bar509"}""");
	}

	private void setupProducer() {
		// set up the Kafka producer properties
		Map<String, Object> senderProperties = KafkaTestUtils.producerProps(kafkaContainer.getBootstrapServers());
		senderProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

		// create a Kafka producer factory
		ProducerFactory<String, String> producerFactory =
				new DefaultKafkaProducerFactory<>(
						senderProperties);

		// create a Kafka template
		template = new KafkaTemplate<>(producerFactory);
		// set the default topic to send to
		template.setDefaultTopic("project-in-0");

		// wait until the partitions are assigned
		for (MessageListenerContainer messageListenerContainer : kafkaListenerEndpointRegistry
				.getListenerContainers()) {
			ContainerTestUtils.waitForAssignment(messageListenerContainer, 1);
		}
	}

	private long countOnIndex() {
		try {
			return client.count(new CountRequest.Builder().index("testindex").build()).count();
		} catch(Exception e) {
			return -1;
		}
	}
}
