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

package org.ct42.fnflow.batchfnlibtest.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ct42.fnflow.batchdlt.BatchElement;
import org.junit.jupiter.api.DisplayName;
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

/**
 * @author Sajjad Safaeian
 */
@SpringBootTest
public class ScriptRunnerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    @TestPropertySource(locations = "classpath:/normal-script.properties")
    protected class NormalJSScript {
        @Autowired
        FunctionCatalog catalog;

        @Test
        @DisplayName("""
                Given a 'ScripRunner' function with a JS script to extract 'records'
                And an input message with 'records' property
                When the 'ScriptRunner' function is executed
                Then the output should contain 2 records
                """)
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

            then(result).hasSize(2);
            then(result.getFirst().getOutput().at("/id").asInt()).isEqualTo(1);
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "cfgfns.ScriptRunner.jsScript.script=test",
    })
    protected class WrongJSScript {
        @Autowired
        FunctionCatalog catalog;

        @Test
        @DisplayName("""
                Given a 'ScripRunner' function with a non-executable JS script
                And an input message
                When the 'ScriptRunner' function is executed
                Then the output should contain 1 error message
                """)
        void wrongScriptTest() throws Exception {
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

            then(result).hasSize(1);
            then(result.getFirst().getOutput()).isNull();
            then(result.getFirst().getError().getMessage()).contains("test is not defined");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "cfgfns.ScriptRunner.jsScript.script='test'",
    })
    protected class WrongResultJSScript {
        @Autowired
        FunctionCatalog catalog;

        @Test
        @DisplayName("""
                Given a 'ScripRunner' function with an executable JS script that returns a non-array result
                And an input message
                When the 'ScriptRunner' function is executed
                Then the output should contain 1 error message
                """)
        void wrongResultScriptTest() throws Exception {
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

            then(result).hasSize(1);
            then(result.getFirst().getOutput()).isNull();
            then(result.getFirst().getError().getMessage()).isNotBlank();
        }
    }

    @SpringBootApplication
    @ComponentScan
    protected static class ScriptRunnerTestApplication {}
}
