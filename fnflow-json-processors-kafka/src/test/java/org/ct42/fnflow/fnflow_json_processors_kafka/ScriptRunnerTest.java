package org.ct42.fnflow.fnflow_json_processors_kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
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

@RegisterReflectionForBinding(classes = KafkaMessageListenerContainer.class)
@Testcontainers
@SpringBootTest(properties = {
    "cfgfns.ScriptRunner.jsScript.script=var inputJson = JSON.parse(input);\\nvar result = [];\\nif (inputJson.records && Array.isArray(inputJson.records)) {\\n    for (var i = 0; i < inputJson.records.length; i++) {\\n        result.push(inputJson.records[i]);\\n    }\\n}\\nresult;",
    "cfgfns.hasValueValidator.idExist.elementPath=/id",
    "org.ct42.fnflow.function.definition=jsScript|idExist",
    "spring.aot.enabled=false"
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

        List<ConsumerRecord<byte[], String>> outputEvents = getConsumerRecords(outputRecords, 2000);
        List<ConsumerRecord<byte[], String>> errorEvents = getConsumerRecords(errorRecords, 2000);

        System.out.println("Sajjad");
    }
}
