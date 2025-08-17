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
 * @author Claas Thiele
 */
@RegisterReflectionForBinding(classes = KafkaMessageListenerContainer.class)
@Testcontainers
@SpringBootTest(properties = {
        "cfgfns.hasValueValidator.idExist.elementPath=/id",
        "cfgfns.ChangeEventEmit.validateEmitter.eventContent=",
        "cfgfns.ChangeEventEmit.validateEmitter.topic=validate-topic",
        "cfgfns.trimNormalizer.idTrim.elementPath=/id",
        "cfgfns.trimNormalizer.idTrim.mode=BOTH",
        "cfgfns.trimNormalizer.nameTrim.elementPath=/name",
        "cfgfns.trimNormalizer.nameTrim.mode=BOTH",
        "cfgfns.ChangeEventEmit.trimEmitter.eventContent=",
        "cfgfns.ChangeEventEmit.trimEmitter.topic=trim-topic",
        "org.ct42.fnflow.function.definition=idExist+validateEmitter|idTrim+trimEmitter|nameTrim"
})
public class EmitterWithNoEmitterCombinationTest extends AbstractKafkaTest {
    public static final String IN_TOPIC = "fnFlowComposedFnBean-in-0";
    public static final String OUTPUT_TOPIC = "fnFlowComposedFnBean-out-0";
    public static final String ERROR_TOPIC = "fnFlowComposedFnBean-out-1";
    public static final String TRIM_TOPIC = "trim-topic";
    public static final String VALIDATE_TOPIC = "validate-topic";

    private final BlockingQueue<ConsumerRecord<byte[], String>> outputRecords = new LinkedBlockingQueue<>();
    private final BlockingQueue<ConsumerRecord<byte[], String>> errorRecords = new LinkedBlockingQueue<>();
    private final BlockingQueue<ConsumerRecord<byte[], String>> trimRecords = new LinkedBlockingQueue<>();
    private final BlockingQueue<ConsumerRecord<byte[], String>> validateRecords = new LinkedBlockingQueue<>();

    @DynamicPropertySource
    static void brokerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafkaContainer::getBootstrapServers);
    }

    @BeforeEach
    void setup() {
        setupProducer(IN_TOPIC);
        setupConsumer(outputRecords, OUTPUT_TOPIC);
        setupConsumer(errorRecords, ERROR_TOPIC);
        setupConsumer(trimRecords, TRIM_TOPIC);
        setupConsumer(validateRecords, VALIDATE_TOPIC);
    }

    @Test
    @DisplayName("""
            GIVEN following pipeline configuration:
                "cfgfns.hasValueValidator.idExist.elementPath=/id",
                "cfgfns.ChangeEventEmit.validateEmitter.eventContent=",
                "cfgfns.ChangeEventEmit.validateEmitter.topic=validate-topic",
                "cfgfns.trimNormalizer.idTrim.elementPath=/id",
                "cfgfns.trimNormalizer.idTrim.mode=BOTH",
                "cfgfns.trimNormalizer.nameTrim.elementPath=/name",
                "cfgfns.trimNormalizer.nameTrim.mode=BOTH",
                "cfgfns.ChangeEventEmit.trimEmitter.eventContent=",
                "cfgfns.ChangeEventEmit.trimEmitter.topic=trim-topic",
                "org.ct42.fnflow.function.definition=idExist+validateEmitter|idTrim+trimEmitter|nameTrim"
            AND following input events
                1={"id":[], "name":"name0"}
                2={"id":["  ID1  "], "name":"  name1  "}
                3={"id":["ID2"], "name":"name2"}
            WHEN the events are meted to the input of the pipeline
            THEN event 1 is landing in the error topic
            AND  event 2 and 3 are landing int the output topic
            AND  event 1,2,3 are landing in the validate topic
            AND  event 1, two times event 2 and two time event 3 are landing in the trim topic
            """)
    void emitterWithNoEmitterCombinationTest() throws Exception {
        template.sendDefault("""
                {"id":[], "name":"name0"}""");
        template.sendDefault("""
                {"id":["  ID1  "], "name":"  name1  "}""");
        template.sendDefault("""
                {"id":["ID2"], "name":"name2"}""");

        List<ConsumerRecord<byte[], String>> outputEvents = getConsumerRecords(outputRecords, 2000);
        List<ConsumerRecord<byte[], String>> trimEvents = getConsumerRecords(trimRecords, 200);
        List<ConsumerRecord<byte[], String>> validateEvents = getConsumerRecords(validateRecords, 200);
        List<ConsumerRecord<byte[], String>> errorEvents = getConsumerRecords(errorRecords, 200);

        then(errorEvents).extracting("value").containsExactly(
                "{\"id\":[], \"name\":\"name0\"}"); //error topic will receive unchanged input, therefore there is the space before "name":

        then(outputEvents).extracting("value").containsExactly(
                "{\"id\":[\"ID1\"],\"name\":\"name1\"}",
                "{\"id\":[\"ID2\"],\"name\":\"name2\"}");

        then(validateEvents).extracting("value").containsExactly(
                "{\"id\":[],\"name\":\"name0\"}",
                "{\"id\":[\"ID1\"],\"name\":\"name1\"}",
                "{\"id\":[\"ID2\"],\"name\":\"name2\"}");

        then(trimEvents).extracting("value").containsExactlyInAnyOrder(
                "{\"id\":[],\"name\":\"name0\"}",
                "{\"id\":[\"  ID1  \"],\"name\":\"name1\"}",
                "{\"id\":[\"ID2\"],\"name\":\"name2\"}",
                "{\"id\":[\"  ID1  \"],\"name\":\"name1\"}",
                "{\"id\":[\"ID2\"],\"name\":\"name2\"}");
    }

}
