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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Sajjad Safaeian
 */
@RegisterReflectionForBinding(classes = KafkaMessageListenerContainer.class)
@Testcontainers
@SpringBootTest(properties = {
    "cfgfns.ScriptRunner.jsScript.script=var inputJson = JSON.parse(input);\\nconst records = inputJson.records;\\nrecords.items = records.items.map(item => ({\\n  ...item,\\n  value: item.value + item.value\\n}));\\nrecords;",
    "cfgfns.hasValueValidator.idExist.elementPath=/id",
    "org.ct42.fnflow.function.definition=jsScript|idExist"
})
public class ScriptRunnerTest extends AbstractKafkaTest {

    public static final String IN_TOPIC = "fnFlowComposedFnBean-in-0";
    public static final String OUTPUT_TOPIC = "fnFlowComposedFnBean-out-0";
    public static final String ERROR_TOPIC = "fnFlowComposedFnBean-out-1";

    private final BlockingQueue<ConsumerRecord<byte[], String>> outputRecords = new LinkedBlockingQueue<>();
    private final BlockingQueue<ConsumerRecord<byte[], String>> errorRecords = new LinkedBlockingQueue<>();

    @DynamicPropertySource
    static void brokerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafkaContainer::getBootstrapServers);
    }

    @BeforeEach
    void setup() {
        setupProducer(IN_TOPIC);
        setupConsumer(outputRecords, OUTPUT_TOPIC);
        setupConsumer(errorRecords, ERROR_TOPIC);
    }

    @Test
    @DisplayName("""
            GIVEN following pipeline configuration:
                "cfgfns.ScriptRunner.jsScript.script=
                    var inputJson = JSON.parse(input);

                    const records = inputJson.records;

                    records.items = records.items.map(item => ({
                      ...item,
                      value: item.value + item.value
                    }));

                    records;
                ",
                "cfgfns.hasValueValidator.idExist.elementPath=/id",
                "org.ct42.fnflow.function.definition=jsScript|idExist"
            AND following input events
                    {
                      "records": {
                        "id": 1,
                        "items": [
                          {"id": 1, "value": "AB"},
                          {"id": 2, "value": "B"}
                        ]
                      }
                    }
                    {
                      "records": {
                        "id": "",
                        "items": [
                          {"id": 3, "value": E"},
                          {"id": 4, "value": "D"}
                        ]
                      }
                    }
            WHEN the events are meted to the input of the pipeline
            THEN jsScript extracts records from event 1, and 2
            AND  idExist reject the second records
            AND  the output topic contains 1 records with ID
            AND  error topic contains 1 input events because of records without an ID
            """)
    void scriptRunnerTest() throws Exception {
        template.sendDefault("""
                    {
                      "records": {
                        "id": 1,
                        "items": [
                          {"id": 1, "value": "AB"},
                          {"id": 2, "value": "B"}
                        ]
                      }
                    }
                """);

        template.sendDefault("""
                    {
                      "records": {
                        "id": "",
                        "items": [
                          {"id": 3, "value": "E"},
                          {"id": 4, "value": "D"}
                        ]
                      }
                    }
                """);

        List<ConsumerRecord<byte[], String>> outputEvents = getConsumerRecords(outputRecords, 3000);
        List<ConsumerRecord<byte[], String>> errorEvents = getConsumerRecords(errorRecords, 1000);

        then(outputEvents)
                .extracting("value")
                .containsExactly("{\"id\":1,\"items\":[{\"id\":1,\"value\":\"ABAB\"},{\"id\":2,\"value\":\"BB\"}]}");

        then(errorEvents)
                .extracting("value")
                .containsExactly("""
                    {
                      "records": {
                        "id": "",
                        "items": [
                          {"id": 3, "value": "E"},
                          {"id": 4, "value": "D"}
                        ]
                      }
                    }
                """);
    }
}
