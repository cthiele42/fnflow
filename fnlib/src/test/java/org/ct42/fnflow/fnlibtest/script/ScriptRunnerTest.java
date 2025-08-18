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

package org.ct42.fnflow.fnlibtest.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ct42.fnflow.fnlib.script.ScriptRunnerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Sajjad Safaeian
 */
@SpringBootTest
public class ScriptRunnerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    @TestPropertySource(locations = "classpath:/normal-js-script.properties")
    protected class NormalJSScript {
        @Autowired
        FunctionCatalog catalog;

        @Test
        @DisplayName("""
                Given a 'ScripRunner' function with a JS script to extract 'records' and duplicate the 'value' of each item
                And an input message with 'records' property
                When the 'ScriptRunner' function is executed
                Then the output should contain records content with duplicated the 'value' of each item
                """)
        void normalScriptTest() throws Exception {
            JsonNode input = mapper.readTree("""
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

            Function<JsonNode, JsonNode> script = catalog.lookup(Function.class, "jsScript");

            JsonNode result = script.apply(input);

            then(result.at("/items/0/value").asText()).isEqualTo("ABAB");
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
                Then the output should raises the 'ScriptRunnerException' exception
                """)
        void wrongScriptTest() throws Exception {
            JsonNode input = mapper.readTree("""
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

            Function<JsonNode, JsonNode> script = catalog.lookup(Function.class, "jsScript");

            thenThrownBy(() -> script.apply(input))
                    .isInstanceOf(ScriptRunnerException.class);
        }
    }

    @Nested
    @TestPropertySource(locations = "classpath:/normal-script.properties")
    protected class WrongResultJSScript {
        @Autowired
        FunctionCatalog catalog;

        @Test
        @DisplayName("""
                Given a 'ScripRunner' function with a JS script to extract 'records' and duplicate the 'value' of each item
                And an input message with 'none-records' property
                When the 'ScriptRunner' function is executed
                Then the output should raises the 'ScriptRunnerException' exception
                """)
        void wrongResultScriptTest() throws Exception {
            JsonNode input = mapper.readTree("""
                    {
                      "none-records": {
                        "id": 1,
                        "items": [
                          {"id": 1, "value": "AB"},
                          {"id": 2, "value": "B"}
                        ]
                      }
                    }
                    """);

            Function<JsonNode, JsonNode> script = catalog.lookup(Function.class, "jsScript");

            thenThrownBy(() -> script.apply(input))
                    .isInstanceOf(ScriptRunnerException.class);
        }
    }

    @Nested
    @TestPropertySource(locations = "classpath:/normal-python-script.properties")
    protected class NormalPythonScript {
        @Autowired
        FunctionCatalog catalog;

        @Test
        @DisplayName("""
                Given a 'ScripRunner' function with a Python script to extract 'records'
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

            Function<List<BatchElement>, List<BatchElement>> script = catalog.lookup(Function.class, "pythonScript");

            List<BatchElement> result = script.apply(List.of(new BatchElement(input)));

            then(result).hasSize(2);
            then(result.getFirst().getOutput().at("/id").asInt()).isEqualTo(1);
        }
    }

    @SpringBootApplication
    @ComponentScan
    protected static class ScriptRunnerTestApplication {}
}
