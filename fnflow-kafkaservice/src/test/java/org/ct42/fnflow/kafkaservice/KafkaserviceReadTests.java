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

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.util.MultiValueMap;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@RegisterReflectionForBinding(classes = KafkaMessageListenerContainer.class)
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
properties = {
	"spring.kafka.producer.batch-size=131072",
	"spring.kafka.producer.compression-type=lz4",
	"spring.kafka.producer.properties.linger.ms=50"
})
class KafkaserviceReadTests {
	public static final String READTOPIC = "testreadtopic";

	@Autowired
	private TestRestTemplate restTemplate;

	@Container
	@ServiceConnection
	static KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka-native:3.8.1");

	private static boolean INITIALIZED = false;
	private static long GAPTS;

	@BeforeEach
	public void setup() throws InterruptedException {
		if(!INITIALIZED) {
			//given 510 messages written with a time gap of 1 second after 300 messages
			String batch1 = "{\"messages\":[" +
					IntStream.range(0, 300)
							.mapToObj(i -> "{\"key\": \"key" + i + "\", \"value\": \"val" + i + "\"}")
							.collect(Collectors.joining(",")) + "]}";
			HttpEntity<String> httpEntity = new HttpEntity<>(batch1, MultiValueMap.fromSingleValue(Map.of("Content-Type", "application/json")));
			ResponseEntity<Void> response = restTemplate.postForEntity("/" + READTOPIC, httpEntity, Void.class);
			then(response.getStatusCode().is2xxSuccessful()).isTrue();
			Thread.sleep(1000);
			GAPTS = System.currentTimeMillis();

			String batch2 = "{\"messages\":[" +
					IntStream.range(300, 510)
							.mapToObj(i -> "{\"key\": \"key" + i + "\", \"value\": \"val" + i + "\"}")
							.collect(Collectors.joining(",")) + "]}";
			httpEntity = new HttpEntity<>(batch2, MultiValueMap.fromSingleValue(Map.of("Content-Type", "application/json")));
			response = restTemplate.postForEntity("/" + READTOPIC, httpEntity, Void.class);
			then(response.getStatusCode().is2xxSuccessful()).isTrue();
			Thread.sleep(100);

			ResponseEntity<TopicInfoDTO> infoResponse = restTemplate.getForEntity("/" + READTOPIC, TopicInfoDTO.class);
			then(infoResponse.getStatusCode().is2xxSuccessful()).isTrue();
			then(infoResponse.getBody().getMessageCount()).isEqualTo(510);

			INITIALIZED = true;
		}
	}

	@Test
	@DisplayName("read all (two batches 500 + 10)")
	void testReadAll()  {
		ResponseEntity<ReadBatchDTO> allBatch1 = restTemplate.getForEntity("/" + READTOPIC + "/0", ReadBatchDTO.class);
		then(allBatch1.getStatusCode().is2xxSuccessful()).isTrue();
		ReadMessage[] messages = allBatch1.getBody().getMessages();
		then(messages).hasSize(500);
		long lastOffset = messages[499].getOffset();
		then(lastOffset).isEqualTo(499);

		ResponseEntity<ReadBatchDTO> allBatch2 = restTemplate.getForEntity("/" + READTOPIC + "/0/500", ReadBatchDTO.class);
		then(allBatch2.getStatusCode().is2xxSuccessful()).isTrue();
		then(allBatch2.getBody().getMessages()).hasSize(10);

		ResponseEntity<ReadBatchDTO> allBatch3 = restTemplate.getForEntity("/" + READTOPIC + "/0/510", ReadBatchDTO.class);
		then(allBatch3.getStatusCode().is2xxSuccessful()).isTrue();
		then(allBatch3.getBody().getMessages()).isEmpty();
	}

	@Test
	@DisplayName("read from offset 100")
	void testReadFromOffset100() {
		ResponseEntity<ReadBatchDTO> from100 = restTemplate.getForEntity("/" + READTOPIC + "/0/100", ReadBatchDTO.class);
		then(from100.getStatusCode().is2xxSuccessful()).isTrue();
		ReadMessage[] messages = from100.getBody().getMessages();
		then(messages).hasSize(410);
		long firstOffset = messages[0].getOffset();
		long lastOffset = messages[409].getOffset();
		then(firstOffset).isEqualTo(100);
		then(lastOffset).isEqualTo(509);
	}

	@Test
	@DisplayName("read from timestamp gapTS")
	void testReadFromTSGapTS() {
		ResponseEntity<ReadBatchDTO> fromGapTS = restTemplate.getForEntity("/" + READTOPIC + "/0/ts" + (GAPTS - 10), ReadBatchDTO.class);
		then(fromGapTS.getStatusCode().is2xxSuccessful()).isTrue();
		ReadMessage[] messages = fromGapTS.getBody().getMessages();
		then(messages).hasSize(210);
		long firstOffset = messages[0].getOffset();
		long lastOffset = messages[209].getOffset();
		then(firstOffset).isEqualTo(300);
		then(lastOffset).isEqualTo(509);
	}

	@Test
	@DisplayName("read from iso timestamp of gapTS")
	void testReadFromIsoOfGapTS() {
		String isoDate = Instant.ofEpochMilli(GAPTS - 10).toString();
		ResponseEntity<ReadBatchDTO> fromGapIso = restTemplate.getForEntity("/" + READTOPIC + "/0/" + isoDate, ReadBatchDTO.class);
		then(fromGapIso.getStatusCode().is2xxSuccessful()).isTrue();
		ReadMessage[] messages = fromGapIso.getBody().getMessages();
		then(messages).hasSize(210);
		long firstOffset = messages[0].getOffset();
		long lastOffset = messages[209].getOffset();
		then(firstOffset).isEqualTo(300);
		then(lastOffset).isEqualTo(509);
	}

	@Test
	@DisplayName("read from offset 0 to timestamp of gapTS")
	void testReadFromOffset0ToTSGapTS() {
		ResponseEntity<ReadBatchDTO> from0ToGapIso = restTemplate.getForEntity("/" + READTOPIC + "/0/0/ts" + (GAPTS - 10), ReadBatchDTO.class);
		then(from0ToGapIso.getStatusCode().is2xxSuccessful()).isTrue();
		ReadMessage[] messages = from0ToGapIso.getBody().getMessages();
		then(messages).hasSize(300);
		long firstOffset = messages[0].getOffset();
		long lastOffset = messages[299].getOffset();
		then(firstOffset).isEqualTo(0);
		then(lastOffset).isEqualTo(299);
	}

	@Test
	@DisplayName("read from timestamp of gapTS to future date (after tomorrow of gapTS) as iso")
	void testReadFromTSGapTSToFuture() {
		String isoAfterTomorrow = Instant.ofEpochMilli(GAPTS + 2*24*3600*1000).toString();
		ResponseEntity<ReadBatchDTO> fromGapTSToFutureDate = restTemplate.getForEntity("/" + READTOPIC + "/0/ts"  + (GAPTS - 10) + "/" + isoAfterTomorrow, ReadBatchDTO.class);
		then(fromGapTSToFutureDate.getStatusCode().is2xxSuccessful()).isTrue();
		ReadMessage[] messages = fromGapTSToFutureDate.getBody().getMessages();
		then(messages).hasSize(210);
		long firstOffset = messages[0].getOffset();
		long lastOffset = messages[209].getOffset();
		then(firstOffset).isEqualTo(300);
		then(lastOffset).isEqualTo(509);
	}

	@ParameterizedTest
	@MethodSource("prepareTopicNotExistSamples")
	void topicIsNotExistTest(String url, HttpMethod httpMethod) {
		ResponseEntity<JsonNode> response = restTemplate.exchange(url, httpMethod, null, JsonNode.class);
		then(response.getStatusCode().is4xxClientError()).isTrue();

		JsonNode message = response.getBody();
        then(message).isNotNull();

		JsonPointer p = JsonPointer.compile("/detail");
		then(message.at(p).asText()).isEqualTo("The topic: not-exist-topic does not exist.");
	}

	private static Stream<Arguments> prepareTopicNotExistSamples() {
		return Stream.of(
				Arguments.of("/not-exist-topic", HttpMethod.GET),
				Arguments.of("/not-exist-topic/0", HttpMethod.GET),
				Arguments.of("/not-exist-topic/0/0", HttpMethod.GET),
				Arguments.of("/not-exist-topic", HttpMethod.DELETE)
		);
	}
}
