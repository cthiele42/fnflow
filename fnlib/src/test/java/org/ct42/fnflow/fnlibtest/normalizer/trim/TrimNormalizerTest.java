package org.ct42.fnflow.fnlibtest.normalizer.trim;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Sajjad Safaeian
 */
@SpringBootTest
public class TrimNormalizerTest {

    private static final String FUNCTION_NAME = "mytrimnormalizer";
    private static final String ELEMENT_PATH = "cfgfns.trimNormalizer.mytrimnormalizer.elementPath=/a/b";
    private static final String MODE = "cfgfns.trimNormalizer.mytrimnormalizer.mode=";
    private static final String SOURCE_METHOD = "org.ct42.fnflow.fnlibtest.normalizer.trim.TrimNormalizerTest#createSamples";

    @Nested
    @TestPropertySource(properties = {ELEMENT_PATH, MODE + "BOTH"})
    protected class BothTrimMode {
        @Autowired
        FunctionCatalog catalog;

        @ParameterizedTest
        @MethodSource(SOURCE_METHOD)
        void name(String json, String elementPath) throws Exception {
            testTrim(json, elementPath, catalog.lookup(Function.class, FUNCTION_NAME), "test");
        }
    }

    @Nested
    @TestPropertySource(properties = {ELEMENT_PATH, MODE + "RIGHT"})
    protected class RightTrimMode {
        @Autowired
        FunctionCatalog catalog;

        @ParameterizedTest
        @MethodSource(SOURCE_METHOD)
        void name(String json, String elementPath) throws Exception {
            testTrim(json, elementPath, catalog.lookup(Function.class, FUNCTION_NAME), "test  ");
        }
    }

    @Nested
    @TestPropertySource(properties = {ELEMENT_PATH, MODE + "LEFT"})
    protected class LeftTrimMode {
        @Autowired
        FunctionCatalog catalog;

        @ParameterizedTest
        @MethodSource(SOURCE_METHOD)
        void name(String json, String elementPath) throws Exception {
            testTrim(json, elementPath, catalog.lookup(Function.class, FUNCTION_NAME), "  test");
        }
    }

    private void testTrim(String json, String elementPath, Function<JsonNode, JsonNode> function, String expectedValue)
            throws Exception {
        JsonPointer pointer = JsonPointer.compile(elementPath);
        JsonNode input = convert(json);

        JsonNode result = function.apply(input);

        then(result.at(pointer).asText()).isEqualTo(expectedValue);
    }

    protected static Stream<Arguments> createSamples() {
        return Stream.of(
                Arguments.of(
            """ 
                       {"a": {"b": "  test  "}}
                       """,
                       "/a/b"),
                Arguments.of(
            """ 
                       {"a": {"b": [{"c": 1}, "  test  "]}}
                       """,
                       "/a/b/1")
        );
    }

    private JsonNode convert(String jsonString) throws Exception {
        return (new ObjectMapper()).readValue(jsonString, JsonNode.class);
    }

    @SpringBootApplication
    @ComponentScan
    protected static class TrimNormalizerTestApplication {}

}
