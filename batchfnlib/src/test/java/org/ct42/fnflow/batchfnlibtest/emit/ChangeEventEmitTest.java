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

package org.ct42.fnflow.batchfnlibtest.emit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ct42.fnflow.batchdlt.HeaderAware;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Claas Thiele
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:/emittest.properties")
public class ChangeEventEmitTest {
    @Autowired
    FunctionCatalog functionCatalog;


    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testInputEmitExistingInput() throws Exception{
        JsonNode input = objectMapper.readTree("""
                {
                    "input": {"foo": "bar"},
                    "matches": []
                }""");
        SimpleFunctionRegistry.FunctionInvocationWrapper wrappedFn = functionCatalog.lookup("input");
        Map<String, Object> headers = null;
        if(wrappedFn.getTarget() instanceof HeaderAware) {
            headers = ((HeaderAware) wrappedFn.getTarget()).headersToBeAdded(input);
        }
        Function<JsonNode, JsonNode> fn = functionCatalog.lookup("input");
        JsonNode output = fn.apply(input);

        then(output.toString()).isEqualTo("""
                {"foo":"bar"}""");
        then(headers).isNotNull()
                .contains(Map.entry("spring.cloud.stream.sendto.destination", "source"))
                .containsKey(KafkaHeaders.KEY);
        then(new String((byte[])headers.get(KafkaHeaders.KEY))).hasSize(20);
    }

    @Test
    void testInputEmitNullInput() throws Exception{
        JsonNode input = objectMapper.readTree("""
                {
                    "input": null,
                    "matches": []
                }""");
        Function<JsonNode, JsonNode> fn = functionCatalog.lookup("input");
        JsonNode output = fn.apply(input);

        then(output).isNull();
    }

    @Test
    void testInputEmitNonExistingInput() throws Exception{
        JsonNode input = objectMapper.readTree("""
                {
                    "matches": []
                }""");
        Function<JsonNode, JsonNode> fn = functionCatalog.lookup("input");
        JsonNode output = fn.apply(input);

        then(output).isNull();
    }

    @Test
    void testEntityEmitExistingMatchWithTextId() throws Exception{
        JsonNode input = objectMapper.readTree("""
                {
                    "input": {"foo": "bar"},
                    "matches": [{"id": "0815", "source":{"foo": "baz"}}]
                }""");
        Map<String, Object> headers = null;
        SimpleFunctionRegistry.FunctionInvocationWrapper wrappedFn = functionCatalog.lookup("entity");
        if(wrappedFn.getTarget() instanceof HeaderAware) {
            headers = ((HeaderAware) wrappedFn.getTarget()).headersToBeAdded(input);
        }
        Function<JsonNode, JsonNode> fn = functionCatalog.lookup("entity");
        JsonNode output = fn.apply(input);

        then(output.toString()).isEqualTo("""
                {"foo":"baz"}""");
        then(headers).isNotNull()
                .doesNotContainKey("spring.cloud.stream.sendto.destination")
                .contains(Map.entry(KafkaHeaders.KEY, "0815".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testEntityEmitExistingMatchWithNumberId() throws Exception{
        JsonNode input = objectMapper.readTree("""
                {
                    "input": {"foo": "bar"},
                    "matches": [{"id": 4711, "source":{"foo": "baz"}}]
                }""");
        Map<String, Object> headers = null;
        SimpleFunctionRegistry.FunctionInvocationWrapper wrappedFn = functionCatalog.lookup("entity");
        if(wrappedFn.getTarget() instanceof HeaderAware) {
            headers = ((HeaderAware) wrappedFn.getTarget()).headersToBeAdded(input);
        }
        Function<JsonNode, JsonNode> fn = functionCatalog.lookup("entity");
        JsonNode output = fn.apply(input);

        then(output.toString()).isEqualTo("""
                {"foo":"baz"}""");
        then(headers).isNotNull()
                .doesNotContainKey("spring.cloud.stream.sendto.destination")
                .contains(Map.entry(KafkaHeaders.KEY, "4711".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testEntityEmitMatchNoSource() throws Exception{
        JsonNode input = objectMapper.readTree("""
                {
                    "input": {"foo": "bar"},
                    "matches": [{"id": "0815"}]
                }""");
        Function<JsonNode, JsonNode> fn = functionCatalog.lookup("entity");
        JsonNode output = fn.apply(input);

        then(output).isNull();
    }

    @Test
    void testEntityEmitMatchNullSource() throws Exception{
        JsonNode input = objectMapper.readTree("""
                {
                    "input": {"foo": "bar"},
                    "matches": [{"id": "0815", "source": null}]
                }""");
        Function<JsonNode, JsonNode> fn = functionCatalog.lookup("entity");
        JsonNode output = fn.apply(input);

        then(output).isNull();
    }

    @Test
    void testEntityEmitNoMatch() throws Exception{
        JsonNode input = objectMapper.readTree("""
                {
                    "input": {"foo": "bar"},
                    "matches": []
                }""");
        Function<JsonNode, JsonNode> fn = functionCatalog.lookup("entity");
        JsonNode output = fn.apply(input);

        then(output).isNull();
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TestConfiguration {}
}
