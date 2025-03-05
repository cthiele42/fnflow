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

package org.ct42.fnflow.batchfnlibtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ct42.fnflow.batchdlt.BatchElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.StoredScript;
import org.opensearch.client.opensearch._types.mapping.KeywordProperty;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.PutScriptRequest;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
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
public class MatchNoMatchTest {
    @Container
    static final OpensearchContainer<?> container = new OpensearchContainer<>("opensearchproject/opensearch:2.19.0");

    @Autowired
    private FunctionCatalog functionCatalog;

    @Autowired
    private OpenSearchClient client;

    @DynamicPropertySource
    static void opensearchProperties(DynamicPropertyRegistry registry) {
        registry.add("opensearch.uris", container::getHttpHostAddress);
    }

    @Test
    @DisplayName("""
            GIVEN a searchtemplate with name 'testtemplate'
            AND an index named 'testindex' with two documents
            AND an input message with an nonmatching id
            WHEN the 'Match' function is executed
            THEN the output will contain an empty 'matches' array
            """)
    public void testMatchFunction() throws Exception {
        //given a search template with name 'testtemplate'
        StoredScript storedScript = new StoredScript.Builder()
                .lang("mustache")
                .source("""
                        {
                          "query": {
                            "terms":{
                              "{{field}}": {{#toJson}}ids{{/toJson}}
                            }
                          }
                        }""")
                .build();
        PutScriptRequest putScriptRequest = new PutScriptRequest.Builder()
                .id("testtemplate")
                .script(storedScript)
                .build();
        client.putScript(putScriptRequest);

        //and given an index with name 'testindex'
        client.indices().create(new CreateIndexRequest.Builder()
                .index("testindex")
                .mappings(new TypeMapping.Builder()
                        .properties("id", new Property.Builder().keyword(new KeywordProperty.Builder().build()).build())
                        .build())
                .build());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode testDoc1 = objectMapper.readValue("""
                {
                    "id": ["ID1", "ID2", "ID3"]
                }""", JsonNode.class);

        IndexRequest<JsonNode> indexRequest1 = new IndexRequest.Builder<JsonNode>()
                .index("testindex")
                .id("doc1")
                .document(testDoc1)
                .build();
        client.index(indexRequest1);

        JsonNode testDoc2 = objectMapper.readValue("""
                {
                    "id": "ID4"
                }""", JsonNode.class);

        IndexRequest<JsonNode> indexRequest2 = new IndexRequest.Builder<JsonNode>()
                .index("testindex")
                .id("doc2")
                .document(testDoc2)
                .build();
        client.index(indexRequest2);

        Thread.sleep(1000);

        Function<List<BatchElement>, List<BatchElement>> fn = functionCatalog.lookup("testmatch");
        JsonNode input = objectMapper.readValue("""
                {
                  "text": ["IDDoesNotExist"],
                  "object":{
                    "otext": "OTest"
                  }
                }
                """, JsonNode.class);
        List<BatchElement> result = fn.apply(List.of(new BatchElement(input)));
        then(result).hasSize(1);
        then(result.getFirst().getOutput().at("/matches").isArray()).isTrue();
        then(result.getFirst().getOutput().at("/matches").size()).isEqualTo(0);
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TestConfiguration {}
}
