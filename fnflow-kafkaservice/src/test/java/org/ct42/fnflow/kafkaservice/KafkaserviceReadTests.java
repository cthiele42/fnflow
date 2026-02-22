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

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Instant;
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
@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
properties = {
	"spring.kafka.producer.batch-size=131072",
	"spring.kafka.producer.compression-type=lz4",
	"spring.kafka.producer.properties.linger.ms=50"
})
class KafkaserviceReadTests {
	public static final String READTOPIC = "testreadtopic";

	@Autowired
	private RestTestClient restClient;

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
			restClient.post()
					.uri("/" + READTOPIC)
					.contentType(MediaType.APPLICATION_JSON)
					.body(batch1)
					.exchange()
					.expectStatus().is2xxSuccessful()
					.expectBody(Void.class);
			Thread.sleep(1000);
			GAPTS = System.currentTimeMillis();

			String batch2 = "{\"messages\":[" +
					IntStream.range(300, 510)
							.mapToObj(i -> "{\"key\": \"key" + i + "\", \"value\": \"val" + i + "\"}")
							.collect(Collectors.joining(",")) + "]}";
			restClient.post()
					.uri("/" + READTOPIC)
					.contentType(MediaType.APPLICATION_JSON)
					.body(batch2)
					.exchange()
					.expectStatus().is2xxSuccessful()
					.expectBody(Void.class);
			Thread.sleep(100);

			EntityExchangeResult<TopicInfoDTO> infoResult = restClient.get()
					.uri("/" + READTOPIC)
					.exchange()
					.expectStatus().is2xxSuccessful()
					.expectBody(TopicInfoDTO.class)
					.returnResult();
			then(infoResult.getResponseBody().getMessageCount()).isEqualTo(510);

			INITIALIZED = true;
		}
	}

	@Test
	@DisplayName("read all (two batches 500 + 10)")
	void testReadAll()  {
		EntityExchangeResult<ReadBatchDTO> readResult = restClient.get()
				.uri("/" + READTOPIC + "/0")
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody(ReadBatchDTO.class)
				.returnResult();

		ReadMessage[] messages = readResult.getResponseBody().getMessages();
		then(messages).hasSize(500);
		long lastOffset = messages[499].getOffset();
		then(lastOffset).isEqualTo(499);

		EntityExchangeResult<ReadBatchDTO> readResult2 = restClient.get()
				.uri("/" + READTOPIC + "/0/500")
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody(ReadBatchDTO.class)
				.returnResult();

		then(readResult2.getResponseBody().getMessages()).hasSize(10);

		EntityExchangeResult<ReadBatchDTO> readResult3 = restClient.get()
				.uri("/" + READTOPIC + "/0/510")
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody(ReadBatchDTO.class)
				.returnResult();

		then(readResult3.getResponseBody().getMessages()).isEmpty();
	}

	@Test
	@DisplayName("read from offset 100")
	void testReadFromOffset100() {
		EntityExchangeResult<ReadBatchDTO> from100 = restClient.get()
				.uri("/" + READTOPIC + "/0/100")
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody(ReadBatchDTO.class)
				.returnResult();
		ReadMessage[] messages = from100.getResponseBody().getMessages();
		then(messages).hasSize(410);
		long firstOffset = messages[0].getOffset();
		long lastOffset = messages[409].getOffset();
		then(firstOffset).isEqualTo(100);
		then(lastOffset).isEqualTo(509);
	}

	@Test
	@DisplayName("read from timestamp gapTS")
	void testReadFromTSGapTS() {
		EntityExchangeResult<ReadBatchDTO> fromGapTS = restClient.get()
				.uri("/" + READTOPIC + "/0/ts" + (GAPTS - 10))
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody(ReadBatchDTO.class)
				.returnResult();

		ReadMessage[] messages = fromGapTS.getResponseBody().getMessages();
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
		EntityExchangeResult<ReadBatchDTO> fromGapIso = restClient.get()
				.uri("/" + READTOPIC + "/0/" + isoDate)
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody(ReadBatchDTO.class)
				.returnResult();

		ReadMessage[] messages = fromGapIso.getResponseBody().getMessages();
		then(messages).hasSize(210);
		long firstOffset = messages[0].getOffset();
		long lastOffset = messages[209].getOffset();
		then(firstOffset).isEqualTo(300);
		then(lastOffset).isEqualTo(509);
	}

	@Test
	@DisplayName("read from offset 0 to timestamp of gapTS")
	void testReadFromOffset0ToTSGapTS() {
		EntityExchangeResult<ReadBatchDTO> from0ToGapIso = restClient.get()
				.uri("/" + READTOPIC + "/0/0/ts" + (GAPTS - 10))
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody(ReadBatchDTO.class)
				.returnResult();

		ReadMessage[] messages = from0ToGapIso.getResponseBody().getMessages();
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
		EntityExchangeResult<ReadBatchDTO> fromGapTSToFutureDate = restClient.get()
				.uri("/" + READTOPIC + "/0/ts"  + (GAPTS - 10) + "/" + isoAfterTomorrow)
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody(ReadBatchDTO.class)
				.returnResult();

		ReadMessage[] messages = fromGapTSToFutureDate.getResponseBody().getMessages();
		then(messages).hasSize(210);
		long firstOffset = messages[0].getOffset();
		long lastOffset = messages[209].getOffset();
		then(firstOffset).isEqualTo(300);
		then(lastOffset).isEqualTo(509);
	}

	@ParameterizedTest
	@MethodSource("prepareTopicNotExistSamples")
	void topicIsNotExistTest(String url, HttpMethod httpMethod) {
		EntityExchangeResult<JsonNode> result = restClient.method(httpMethod)
				.uri(url)
				.exchange()
				.expectStatus().is4xxClientError()
				.expectBody(JsonNode.class)
				.returnResult();

		JsonNode message = result.getResponseBody();
        then(message).isNotNull();

		JsonPointer p = JsonPointer.compile("/detail");
		then(message.at(p).asString()).isEqualTo("The topic: not-exist-topic does not exist.");
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
