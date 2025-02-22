package org.ct42.fnflow.batchdlttest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.ct42.fnflow.batchdlt.BatchElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
@EmbeddedKafka(bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers", partitions = 1)
@TestPropertySource(properties = {
        "spring.cloud.function.definition=fnFlowComposedFnBean",
        "spring.cloud.stream.default.group=test",
        "org.ct42.fnflow.function.definition=jbifun|jbifun2|jbatchfun"
})
public class BatchDltTest {
    public static final String IN_TOPIC = "fnFlowComposedFnBean-in-0";
    public static final String OUT_TOPIC = "fnFlowComposedFnBean-out-0";
    public static final String DLT_TOPIC = "fnFlowComposedFnBean-out-1";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

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
    public void testBatchDltCompose() throws Exception{
        for (int i = 0; i < 10; i++) {
            template.sendDefault("{\"text\": \"T" + i + "\"}");
        }
        List<ConsumerRecord<String, String>> results = new ArrayList<>();
        while (true) {
            ConsumerRecord<String, String> received =
                    inRecords.poll(200, TimeUnit.MILLISECONDS);
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
                {"text":"BLEN2: MO2: MO: T0"}""",
                """
                {"text":"BLEN2: MO2: MO: T1"}""",
                """
                {"text":"BLEN2: MO2: MO: T4"}""",
                """
                {"text":"BLEN2: MO2: MO: T9"}"""
        );
        then(errors).extracting(ConsumerRecord::value).filteredOn(s -> !s.contains("MO2")).containsExactly(
                """
                {"text":"T3"}""",
                """
                {"text":"MO: T5"}""",
                """
                {"text":"T7"}""",
                """
                {"text":"MO: T8"}"""
        );
        then(errors).extracting(ConsumerRecord::value).filteredOn(s -> s.contains("MO2")).containsExactly(
                """
                {"text":"MO2: MO: T2"}""",
                """
                {"text":"MO2: MO: T6"}"""
        );
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TestConfiguration {
    }

    @Component("jbifun")
    protected final static class BiFunLogic implements Function<JsonNode, JsonNode> {
        @Override
        public JsonNode apply(JsonNode n) {
            String s = n.get("text").textValue();
            if (s.contains("T3") || s.contains("T7")) throw new RuntimeException("ERR");
            ((ObjectNode)n).put("text", "MO: " + s);
            return n;
        }
    }

    @Component("jbifun2")
    protected final static class BiFunLogic2 implements Function<JsonNode, JsonNode> {
        @Override
        public JsonNode apply(JsonNode n) {
            String s = n.get("text").textValue();
            if (s.contains("T5") || s.contains("T8")) throw new RuntimeException("ERR2");
            ((ObjectNode)n).put("text", "MO2: " + s);
            return n;
        }
    }

    @Component("jbatchfun")
    protected final static class BatchFun implements Function<List<BatchElement>, List<BatchElement>> {
        /**
         * @param b the batch; will be not empty
         * @return the batch with processed values
         */
        @Override
        public List<BatchElement> apply(List<BatchElement> b) {
            b.forEach(e -> {
                JsonNode inputRoot = e.getInput();
                String s = inputRoot.get("text").textValue();
                if (s.contains("T2") || s.contains("T6")) {
                    e.processWithError(new IllegalStateException("ERRB"));
                } else {
                    ((ObjectNode)inputRoot).put("text", "BLEN" + b.size() + ": " + s);
                e.processWithOutput(inputRoot);
                }
            });
            return b;
        }
    }

    private void setupConsumer(BlockingQueue<ConsumerRecord<String, String>> queue, String topic) {
        // set up the Kafka consumer properties
        Map<String, Object> consumerProperties =
                KafkaTestUtils.consumerProps("sender", "false", embeddedKafka);

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
        Map<String, Object> senderProperties =
                KafkaTestUtils.producerProps(
                        embeddedKafka.getBrokersAsString());

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
            ContainerTestUtils.waitForAssignment(messageListenerContainer,
                    embeddedKafka.getPartitionsPerTopic());
        }
    }
}
