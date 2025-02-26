package org.ct42.fnflow.batchdlt_cfgfns_example;

import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import org.springframework.test.context.TestPropertySource;
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

@Testcontainers
@SpringBootTest
@RegisterReflectionForBinding(classes = KafkaMessageListenerContainer.class)
@TestPropertySource(properties = {
		"spring.cloud.stream.default.group=xmpl",
		"org.ct42.fnflow.default.batch.size=2"
})
class BatchdltCfgfnsExampleApplicationTests {
	public static final String IN_TOPIC = "fnFlowComposedFnBean-in-0";
	public static final String OUT_TOPIC = "fnFlowComposedFnBean-out-0";
	public static final String DLT_TOPIC = "fnFlowComposedFnBean-out-1";

	@Container
	static KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka-native:3.8.1");

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.cloud.stream.kafka.binder.brokers", kafkaContainer::getBootstrapServers);
	}

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
	void testBatchDLTCfgFns() throws Exception {
		for (int i = 0; i < 10; i++) {
			template.sendDefault("{\"text\": \"T" + i + "\"}");
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
		then(results).extracting(ConsumerRecord::value).containsExactly(
				"""
                {"text":"BLEN2 PRFX: T0"}""",
				"""
                {"text":"BLEN2 PRFX: T1"}""",
				"""
                {"text":"BLEN2 PRFX: T3"}""",
				"""
                {"text":"BLEN2 PRFX: T4"}""",
				"""
                {"text":"BLEN2 PRFX: T5"}""",
				"""
                {"text":"BLEN2 PRFX: T7"}""",
				"""
                {"text":"BLEN2 PRFX: T8"}""" ,
				"""
                {"text":"BLEN1 PRFX: T9"}"""
		);
		then(errors).extracting(ConsumerRecord::value).containsExactlyInAnyOrder(
				"""
                {"text": "T2"}""",
				"""
				{"text": "T6"}"""
		);
	}

	private void setupConsumer(BlockingQueue<ConsumerRecord<String, String>> queue, String topic) {
		// set up the Kafka consumer properties
		Map<String, Object> consumerProperties =
				KafkaTestUtils.consumerProps(kafkaContainer.getBootstrapServers(), "sender");

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
