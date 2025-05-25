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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.ComponentScan;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
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

    @ParameterizedTest
    @MethodSource("createAmbiguousMatchSamples")
    void testReduceOneAmbiguousMatch(String inputJson, String expectedJson) throws Exception {
        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "reduce");
        JsonNode input = mapper.readTree(inputJson);

        then(function.apply(input).toString()).isEqualToIgnoringWhitespace(expectedJson);
    }

    private static Stream<Arguments> createAmbiguousMatchSamples() {
        return Stream.of(
                Arguments.of(
                    """
                    {
                        "input": {"id": "ID1"},
                        "matches": [
                            {"id": "match1", "score":0.99,"source":{"foo":"bar"}},
                            {"id": "match2", "score":0.97,"source":{"foo":"baz"}}
                        ]
                    }
                    """,
                    """
                    {
                        "input": {"id": "ID1"},
                        "matches": [
                            {"id": "match1", "score":0.99,"source":{"foo":"bar"}}
                        ]
                    }
                    """
                ),
                Arguments.of(
                    """
                    {
                        "input": {"id": "ID1"},
                        "matches": [
                            {"id": "match1", "score":0.98,"source":{"foo":"bar"}},
                            {"id": "match2", "score":0.99,"source":{"foo":"baz"}},
                            {"id": "match3", "score":0.99,"source":{"foo":"bad"}}
                        ]
                    }
                    """,
                    """
                    {
                        "input": {"id": "ID1"},
                        "matches": [
                            {"id": "match2", "score":0.99,"source":{"foo":"baz"}}
                        ]
                    }
                    """
                ),
                Arguments.of(
                        """
                        {
                            "input": {"id": "ID1"},
                            "matches": [
                                {"id": "match1", "score":0.99,"source":{"foo":"bar"}},
                                {"id": "match2", "source":{"foo":"baz"}}
                            ]
                        }
                        """,
                        """
                        {
                            "input": {"id": "ID1"},
                            "matches": [
                                {"id": "match2", "source":{"foo":"baz"}}
                            ]
                        }
                        """
                ),
                Arguments.of(
                        """
                        {
                            "input": {"id": "ID1"},
                            "matches": [
                                {"id": "match1", "score":0.99,"source":{"foo":"bar"}},
                                {"id": "match2", "score":"", "source":{"foo":"baz"}}
                            ]
                        }
                        """,
                        """
                        {
                            "input": {"id": "ID1"},
                            "matches": [
                                {"id": "match2", "score":"", "source":{"foo":"baz"}}
                            ]
                        }
                        """
                ),
                Arguments.of(
                        """
                        {
                            "input": {"id": "ID1"},
                            "matches": [
                                {"id": "match1", "score":0.99,"source":{"foo":"bar"}},
                                {"id": "match2", "score":"a", "source":{"foo":"baz"}}
                            ]
                        }
                        """,
                        """
                        {
                            "input": {"id": "ID1"},
                            "matches": [
                                {"id": "match2", "score":"a", "source":{"foo":"baz"}}
                            ]
                        }
                        """
                ),
                Arguments.of(
                        """
                        {
                            "input": {"id": "ID1"},
                            "matches": [
                                {"id": "match1", "score":-100, "source":{"foo":"bar"}},
                                {"id": "match2", "score":-99, "source":{"foo":"baz"}},
                                {"id": "match3", "score":-101, "source":{"foo":"bad"}}
                            ]
                        }
                        """,
                        """
                        {
                            "input": {"id": "ID1"},
                            "matches": [
                                {"id": "match2", "score":-99, "source":{"foo":"baz"}}
                            ]
                        }
                        """
                )
        );
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
                {"input":{"id":"ID1"},"matches":[{"source":{}}]}""");
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
