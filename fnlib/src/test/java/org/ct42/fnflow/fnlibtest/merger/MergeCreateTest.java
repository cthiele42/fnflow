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

package org.ct42.fnflow.fnlibtest.merger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.ComponentScan;

import java.util.function.Function;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Claas Thiele
 */
@SpringBootTest(
        properties = {
                "cfgfns.MergeCreate.createmerge.mappings.0.from=/id",
                "cfgfns.MergeCreate.createmerge.mappings.0.to=/identifier/id",
                "cfgfns.MergeCreate.createmerge.mappings.1.from=/id",
                "cfgfns.MergeCreate.createmerge.mappings.1.to=/ID",
                "cfgfns.MergeCreate.createmerge.mappings.2.from=/doesNotExist",
                "cfgfns.MergeCreate.createmerge.mappings.2.to=/foo"
        }
)
public class MergeCreateTest {
    @Autowired
    FunctionCatalog catalog;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("""
            GIVEN a merge config trying to merge an property existing in the target
            AND trying to merge a target property in an object where the object does not exist
            AND trying to merge an input property that does not exist
            WHEN the MergeCreate function is executed
            THEN the existing property is not merged
            AND the non existing property is created together with its container object
            AND the non existing input property is ignored, no exception is thrown
            """)
    void testMergeCreate() throws Exception {
        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "createmerge");
        JsonNode input = mapper.readTree("""
                {
                    "input": {"id": "ID1"},
                    "matches": [
                        {"id": "match!", "score":1.0,"source":{"id":"TARGET_ID"}}
                    ]
                }""");

        then(function.apply(input).toString()).isEqualTo("""
                {"input":{"id":"ID1"},"matches":[{"id":"match!","score":1.0,"source":{"id":"TARGET_ID","identifier":{"id":"ID1"},"ID":"ID1"}}]}""");
    }

    @Test
    @DisplayName("""
            GIVEN a merge config trying to merge an property existing in the target
            AND trying to merge a target property in an object where the object exist
            AND trying to merge an input property that does not exist
            WHEN the MergeCreate function is executed
            THEN the existing property is not merged
            AND the non existing property is created together with its container object
            AND the non existing input property is ignored, no exception is thrown
            """)
    void testMergeCreateIntoExistingObject() throws Exception {
        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "createmerge");
        JsonNode input = mapper.readTree("""
                {
                    "input": {"id": "ID1"},
                    "matches": [
                        {"id": "match!", "score":1.0,"source":{"id":"TARGET_ID","identifier":{"foo":42}}}
                    ]
                }""");

        then(function.apply(input).toString()).isEqualTo("""
                {"input":{"id":"ID1"},"matches":[{"id":"match!","score":1.0,"source":{"id":"TARGET_ID","identifier":{"foo":42,"id":"ID1"},"ID":"ID1"}}]}""");
    }

    @Test
    @DisplayName("""
            GIVEN a no match input with input being null and the matches array being empty
            WHEN the MergeCreate function is executed
            THEN the result will be an unchanged input
            AND no exception is thrown
            """)
    void testMergeCreateNoMatch() throws Exception {
        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "createmerge");
        JsonNode input = mapper.readTree("""
                {
                    "input": null,
                    "matches": []
                }""");

        then(function.apply(input).toString()).isEqualTo("""
                {"input":null,"matches":[]}""");
    }

    @Test
    @DisplayName("""
            GIVEN an input where the first matches element has no source property
            WHEN the MergeCreate function is executed
            THEN the result will be an unchanged input
            AND no exception is thrown
            """)
    void testMergeNoSource() throws Exception {
        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "createmerge");
        JsonNode input = mapper.readTree("""
                {
                    "input": {"id": "ID1"},
                    "matches": [
                        {"id": "match!", "score":1.0}
                    ]
                }""");

        then(function.apply(input).toString()).isEqualTo("""
                {"input":{"id":"ID1"},"matches":[{"id":"match!","score":1.0}]}""");
    }

    @SpringBootApplication
    @ComponentScan
    protected static class MergeCreateTestApplication {}
}
