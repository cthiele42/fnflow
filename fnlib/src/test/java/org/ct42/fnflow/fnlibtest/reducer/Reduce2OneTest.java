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

package org.ct42.fnflow.fnlibtest.reducer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.ComponentScan;

import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Claas Thiele
 */
@SpringBootTest(
        properties = {
                "cfgfns.Reduce2One.reduce.no-config" //if a function has no configuration, at least one property has to be provided, doesn't have to have a value
        }
)
public class Reduce2OneTest {
    @Autowired
    FunctionCatalog catalog;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testReduceOneMatch() throws Exception {
        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "reduce");
        JsonNode input = mapper.readTree("""
                {
                    "input": {"id": "ID1"},
                    "matches": [
                        {"id": "match!", "score":1.0,"source":{"foo":"bar"}}
                    ]
                }""");

        then(function.apply(input).toString()).isEqualTo("""
                {"input":{"id":"ID1"},"matches":[{"id":"match!","score":1.0,"source":{"foo":"bar"}}]}""");
    }

    @Test
    void testReduceOneAmbiguousMatch() throws Exception {
        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "reduce");
        JsonNode input = mapper.readTree("""
                {
                    "input": {"id": "ID1"},
                    "matches": [
                        {"id": "match1", "score":1.0,"source":{"foo":"bar"}},
                        {"id": "match2", "score":1.0,"source":{"foo":"baz"}}
                    ]
                }""");

        then(function.apply(input).toString()).isEqualTo("""
                {"input":null,"matches":[]}""");
    }

    @Test
    void testReduceOneNoMatch() throws Exception {
        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "reduce");
        JsonNode input = mapper.readTree("""
                {
                    "input": {"id": "ID1"},
                    "matches": []
                }""");

        then(function.apply(input).toString()).isEqualTo("""
                {"input":{"id":"ID1"},"matches":[{}]}""");
    }

    @Test
    void testReduceOneNoMatchesArray() throws Exception {
        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "reduce");
        JsonNode input = mapper.readTree("""
                {
                    "input": {"id": "ID1"}
                }""");
        thenThrownBy(() -> function.apply(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid input, no matches array found");
    }

    @Test
    void testReduceOneMatchesOfWrongType() throws Exception {
        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "reduce");
        JsonNode input = mapper.readTree("""
                {
                    "input": {"id": "ID1"},
                    "matches": "I am wrong here!"
                }""");
        thenThrownBy(() -> function.apply(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid input, no matches array found");
    }

    @SpringBootApplication
    @ComponentScan
    protected static class ReducerTestApplication {}
}
