package org.ct42.fnflow.fnflow_json_processors_kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.StoredScript;
import org.opensearch.client.opensearch._types.mapping.KeywordProperty;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.PutScriptRequest;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.testcontainers.OpensearchContainer;
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

@RegisterReflectionForBinding(classes = KafkaMessageListenerContainer.class)
@Testcontainers
@SpringBootTest(properties = {
		"cfgfns.hasValueValidator.idExist.elementPath=/id",
		"cfgfns.Match.idMatch.index=testindex",
		"cfgfns.Match.idMatch.template=testtemplate",
		"cfgfns.Match.idMatch.paramsFromInput.ids=/id",
		"cfgfns.Match.idMatch.literalParams.field=id",
		"org.ct42.fnflow.function.definition=idExist|idMatch"
})
class FnflowJsonProcessorsKafkaApplicationTests {
	public static final String IN_TOPIC = "fnFlowComposedFnBean-in-0";
	public static final String OUT_TOPIC = "fnFlowComposedFnBean-out-0";
	public static final String DLT_TOPIC = "fnFlowComposedFnBean-out-1";

	@Container
	static final OpensearchContainer<?> container = new OpensearchContainer<>("opensearchproject/opensearch:2.19.0");

	@Container
	static KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka-native:3.8.1");

	@Autowired
	private OpenSearchClient client;

	@DynamicPropertySource
	static void opensearchProperties(DynamicPropertyRegistry registry) {
		registry.add("opensearch.uris", container::getHttpHostAddress);
		registry.add("spring.cloud.stream.kafka.binder.brokers", kafkaContainer::getBootstrapServers);
	}

	@Autowired
	private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

	private KafkaTemplate<String, String> template;
	private final BlockingQueue<ConsumerRecord<String, String>> inRecords = new LinkedBlockingQueue<>();
	private final BlockingQueue<ConsumerRecord<String, String>> errRecords = new LinkedBlockingQueue<>();

	@BeforeEach
	void setup() throws Exception{
		StoredScript storedScript = new StoredScript.Builder()
				.lang("mustache")
				.source("""
                        {
                          "query": {
                            "terms":{
                              "{{field}}": {{#toJson}}ids{{/toJson}}
                            }
                          }
                        }""")
				.build();
		PutScriptRequest putScriptRequest = new PutScriptRequest.Builder()
				.id("testtemplate")
				.script(storedScript)
				.build();
		client.putScript(putScriptRequest);

		//and given an index with name 'testindex'
		client.indices().create(new CreateIndexRequest.Builder()
				.index("testindex")
				.mappings(new TypeMapping.Builder()
						.properties("id", new Property.Builder().keyword(new KeywordProperty.Builder().build()).build())
						.build())
				.build());

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode testDoc1 = objectMapper.readValue("""
                {
                    "id": ["ID1", "ID2", "ID3"]
                }""", JsonNode.class);

		IndexRequest<JsonNode> indexRequest1 = new IndexRequest.Builder<JsonNode>()
				.index("testindex")
				.id("doc1")
				.document(testDoc1)
				.build();
		client.index(indexRequest1);

		JsonNode testDoc2 = objectMapper.readValue("""
                {
                    "id": "ID4"
                }""", JsonNode.class);

		IndexRequest<JsonNode> indexRequest2 = new IndexRequest.Builder<JsonNode>()
				.index("testindex")
				.id("doc2")
				.document(testDoc2)
				.build();
		client.index(indexRequest2);

		Thread.sleep(1000);

		setupProducer();
		setupConsumer(inRecords, OUT_TOPIC);
		setupConsumer(errRecords, DLT_TOPIC);
	}

	@Test
	void testProcessing() throws Exception {
		for (int i = 0; i < 1000; i++) {
			if(i % 3 == 0) {
				template.sendDefault("{\"id\": []}");
			} else {
				template.sendDefault("{\"id\": [\"T" + i + "\"]}");
			}
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

		then(results).hasSize(666);
		then(errors).hasSize(334);
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
