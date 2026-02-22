package org.ct42.fnflow.fnflow_json_processors_kafka;

import org.opensearch.client.opensearch._types.BuiltinScriptLanguage;
import org.opensearch.client.opensearch._types.ScriptLanguage;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
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
		"cfgfns.hasValueValidator.idExist.elementPath=/id",
		"cfgfns.Match.idMatch.index=testindex",
		"cfgfns.Match.idMatch.template=testtemplate",
		"cfgfns.Match.idMatch.paramsFromInput.ids=/id",
		"cfgfns.Match.idMatch.literalParams.field=id",
		"org.ct42.fnflow.function.definition=idExist|idMatch"
})
class FnflowJsonProcessorsKafkaApplicationTests extends AbstractKafkaTest {
	public static final String IN_TOPIC = "fnFlowComposedFnBean-in-0";
	public static final String OUT_TOPIC = "fnFlowComposedFnBean-out-0";
	public static final String DLT_TOPIC = "fnFlowComposedFnBean-out-1";

	@Container
	static final OpenSearchContainer<?> container = new OpenSearchContainer<>("opensearchproject/opensearch:3.5.0");

	@Autowired
	private OpenSearchClient client;

	@DynamicPropertySource
	static void opensearchProperties(DynamicPropertyRegistry registry) {
		registry.add("opensearch.uris", container::getHttpHostAddress);
		registry.add("spring.cloud.stream.kafka.binder.brokers", kafkaContainer::getBootstrapServers);
	}

	private final BlockingQueue<ConsumerRecord<byte[], String>> inRecords = new LinkedBlockingQueue<>();
	private final BlockingQueue<ConsumerRecord<byte[], String>> errRecords = new LinkedBlockingQueue<>();

	@BeforeEach
	void setup() throws Exception{
		StoredScript storedScript = new StoredScript.Builder()
				.lang(ScriptLanguage.builder().builtin(BuiltinScriptLanguage.Mustache).build())
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

		setupProducer(IN_TOPIC);
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
		List<ConsumerRecord<byte[], String>> results = getConsumerRecords(inRecords, 2000);
		List<ConsumerRecord<byte[], String>> errors = getConsumerRecords(errRecords, 200);

		then(results).hasSize(666);
		then(errors).hasSize(334);
	}
}
