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

package org.ct42.fnflow.fnlibtest.validator.hasvalue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ct42.fnflow.fnlib.validator.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.ComponentScan;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;


/**
 * @author Sajjad Safaeian
 */
@SpringBootTest(
    properties = {
        "cfgfns.hasValueValidator.myvaluecheck.elementPath=/a/b"
    }
)
class HasValueValidatorTest {

    @Autowired
    FunctionCatalog catalog;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testReplace() {
        thenThrownBy(
                () -> new SpringApplicationBuilder(HasValueValidatorTestApplication.class)
                        .properties("cfgfns.hasValueValidator.myvaluecheck.elementPath=a").run()
        )
        .hasRootCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("createExceptionalSamples")
    void exceptionalSituationTest(String json, String exceptionMessage) throws Exception {
        JsonNode input = convert(json);

        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "myvaluecheck");

        thenThrownBy(() -> function.apply(input))
                .isInstanceOf(ValidationException.class)
                .hasMessage(exceptionMessage);
    }

    private static Stream<Arguments> createExceptionalSamples() {
        return Stream.of(
                Arguments.of(
                        """ 
                                {}
                                """, "The element is not Exist."),
                Arguments.of(
                        """ 
                                {"a": 1}
                                """, "The element is not Exist."),
                Arguments.of(
                        """ 
                                {"a": {"b": null}}
                                """, "The value is null."),
                Arguments.of(
                        """ 
                                {"a": {"b": ""}}
                                """, "The value is empty text."),
                Arguments.of(
                        """ 
                                {"a": {"b": [{"c": 1}, "", null]}}
                                """, "The array element has not value."),
                Arguments.of(
                        """ 
                                {"a": {"b": [{"c": 1}, {"d": null}]}}
                                """, "The array element has not value."),
                Arguments.of(
                        """ 
                                {"a": {"b": {"c": 1}}}
                                """, "Object type is not acceptable.")
        );
    }

    @ParameterizedTest
    @MethodSource("createNormalSamples")
    void normalSituationTest(String json) throws Exception {
        JsonNode input = convert(json);

        Function<JsonNode, JsonNode> function = catalog.lookup(Function.class, "myvaluecheck");
        JsonNode result = function.apply(input);

        then(result.toString()).isEqualToIgnoringWhitespace(json.trim());
    }

    private static Stream<Arguments> createNormalSamples() {
        return Stream.of(
                Arguments.of(
                        """ 
                                {"a": {"b": 1}}
                                """),
                Arguments.of(
                        """ 
                                {"a": {"b": "1"}}
                                """),
                Arguments.of(
                        """ 
                                {"a": {"b": false}}
                                """),
                Arguments.of(
                        """ 
                                {"a": {"b": [{"c": 1}, null, "1"]}}
                                """)
        );
    }


    private JsonNode convert(String jsonString) throws Exception {
        return mapper.readValue(jsonString, JsonNode.class);
    }

    @SpringBootApplication
    @ComponentScan
    protected static class HasValueValidatorTestApplication {}

}