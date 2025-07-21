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
    "cfgfns.ScriptRunner.jsScript.script=var inputJson = JSON.parse(input);\\nvar result = [];\\nif (inputJson.records && Array.isArray(inputJson.records)) {\\n    for (var i = 0; i < inputJson.records.length; i++) {\\n        result.push(inputJson.records[i]);\\n    }\\n}\\nresult;",
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
                    var result = [];
                    if (inputJson.records && Array.isArray(inputJson.records)) {
                        for (var i = 0; i < inputJson.records.length; i++) {
                            result.push(inputJson.records[i]);
                        }
                    }
                    result;
                ",
                "cfgfns.hasValueValidator.idExist.elementPath=/id",
                "org.ct42.fnflow.function.definition=jsScript|idExist"
            AND following input events
                    {
                      "records": [
                        {"id": 1, "value": "A"},
                        {"id": 2, "value": "B"},
                        {"id": "", "value": "C"}
                      ]
                    }
                    {
                      "records": [
                        {"id": 4, "value": "D"},
                        {"id": "", "value": "E"}
                      ]
                    }
            WHEN the events are meted to the input of the pipeline
            THEN jsScript extracts 3 records from event 1, and 2 records from event 2
            AND  idExist reject 2 records
            AND  the output topic contains 4 records with ID
            AND  error topic contains 2 input events because of records without an ID
            """)
    void scriptRunnerTest() throws Exception {
        template.sendDefault("""
                    {
                      "records": [
                        {"id": 1, "value": "A"},
                        {"id": 2, "value": "B"},
                        {"id": "", "value": "C"}
                      ]
                    }
                """);

        template.sendDefault("""
                    {
                      "records": [
                        {"id": 4, "value": "D"},
                        {"id": "", "value": "E"}
                      ]
                    }
                """);

        List<ConsumerRecord<byte[], String>> outputEvents = getConsumerRecords(outputRecords, 3000);
        List<ConsumerRecord<byte[], String>> errorEvents = getConsumerRecords(errorRecords, 1000);

        then(outputEvents)
                .extracting("value")
                .containsExactly("{\"id\":1,\"value\":\"A\"}",
                        "{\"id\":2,\"value\":\"B\"}",
                        "{\"id\":4,\"value\":\"D\"}");

        then(errorEvents)
                .extracting("value")
                .containsExactly("""
                    {
                      "records": [
                        {"id": 1, "value": "A"},
                        {"id": 2, "value": "B"},
                        {"id": "", "value": "C"}
                      ]
                    }
                """,
                """
                    {
                      "records": [
                        {"id": 4, "value": "D"},
                        {"id": "", "value": "E"}
                      ]
                    }
                """);
    }
}
