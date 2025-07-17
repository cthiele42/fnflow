package org.ct42.fnflow.batchfnlibtest.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ct42.fnflow.batchdlt.BatchElement;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
public class ScriptRunnerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    @TestPropertySource(locations = "classpath:/normal-script.properties")
    protected class NormalScript {
        @Autowired
        FunctionCatalog catalog;

        @Test
        void normalScriptTest() throws Exception {
            JsonNode input = mapper.readTree("""
                    {
                      "records": [
                        {"id": 1, "value": "A"},
                        {"id": 2, "value": "B"}
                      ]
                    }
                    """);

            Function<List<BatchElement>, List<BatchElement>> script = catalog.lookup(Function.class, "jsScript");

            List<BatchElement> result = script.apply(List.of(new BatchElement(input)));

            then(result).isNotEmpty();
        }
    }

    @SpringBootApplication
    @ComponentScan
    protected static class ScriptRunnerTestApplication {}
}
