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

package org.ct42.fnflow.batchfnlibtest.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ct42.fnflow.batchdlt.BatchElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Claas Thiele
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(locations = "classpath:/matchtest.properties")
public class MatchErrorTest {
    @Container
    static final OpensearchContainer<?> container = new OpensearchContainer<>("opensearchproject/opensearch:2.19.0");

    @Autowired
    private FunctionCatalog functionCatalog;

    @DynamicPropertySource
    static void opensearchProperties(DynamicPropertyRegistry registry) {
        registry.add("opensearch.uris", container::getHttpHostAddress);
    }

    @Test
    @DisplayName("""
            GIVEN no searchtemplate exists
            WHEN the 'Match' function is executed
            THEN it results to no output
            AND an error is set in the result
            """)
    public void testMatchFunction() throws Exception {
        //given no search template with name 'testtemplate'
        ObjectMapper objectMapper = new ObjectMapper();
        Function<List<BatchElement>, List<BatchElement>> fn = functionCatalog.lookup("testmatch");
        JsonNode input = objectMapper.readValue("""
                {
                  "text": "ID1",
                  "object":{
                    "otext": "OTest"
                  }
                }
                """, JsonNode.class);
        List<BatchElement> result = fn.apply(List.of(new BatchElement(input)));
        then(result).hasSize(1);
        then(result.getFirst().getOutput()).isNull();
        then(result.getFirst().getError().getMessage()).contains("Request failed");
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TestConfiguration {}
}
