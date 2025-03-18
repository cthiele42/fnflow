package org.ct42.fnflow.fnlibtest.normalizer.pad;

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
public class PadNormalizerTest {

    private static final String FUNCTION_NAME = "mypadnormalizer";
    private static final String ELEMENT_PATH = "cfgfns.padNormalizer.mypadnormalizer.elementPath=/a/b";
    private static final String PAD = "cfgfns.padNormalizer.mypadnormalizer.pad=";
    private static final String LENGTH = "cfgfns.padNormalizer.mypadnormalizer.length=8";
    private static final String FILLER_CHARACTER = "cfgfns.padNormalizer.mypadnormalizer.fillerCharacter=0";

    @Nested
    @TestPropertySource(properties = {ELEMENT_PATH, PAD + "RIGHT", LENGTH, FILLER_CHARACTER})
    protected class RightPad {
        @Autowired
        FunctionCatalog catalog;

        @ParameterizedTest
        @MethodSource("rightPadSamples")
        void name(String json, String elementPath, String expectedValue) throws Exception {
            testPad(json, elementPath, catalog.lookup(Function.class, FUNCTION_NAME), expectedValue);
        }

        private static Stream<Arguments> rightPadSamples() {
            return createSamples().map(args -> Arguments.of(args.get()[0], args.get()[1], args.get()[3]));
        }
    }

    @Nested
    @TestPropertySource(properties = {ELEMENT_PATH, PAD + "LEFT", LENGTH, FILLER_CHARACTER})
    protected class LeftPad {
        @Autowired
        FunctionCatalog catalog;

        @ParameterizedTest
        @MethodSource("leftPadSamples")
        void name(String json, String elementPath, String expectedValue) throws Exception {
            testPad(json, elementPath, catalog.lookup(Function.class, FUNCTION_NAME), expectedValue);
        }

        private static Stream<Arguments> leftPadSamples() {
            return createSamples().map(args -> Arguments.of(args.get()[0], args.get()[1], args.get()[2]));
        }

    }

    private void testPad(String json, String elementPath, Function<JsonNode, JsonNode> function, String expectedValue)
            throws Exception {
        JsonPointer pointer = JsonPointer.compile(elementPath);
        JsonNode input = convert(json);

        JsonNode result = function.apply(input);

        then(result.at(pointer).toString()).isEqualTo(expectedValue);
    }

    protected static Stream<Arguments> createSamples() {
        return Stream.of(
                Arguments.of(
            """ 
                       {"a": {"b": "12345"}}
                       """, "/a/b", """
                       "00012345"\
                       """, """
                       "12345000"\
                       """),
                Arguments.of(
            """ 
                       {"a": {"b": [{"c": 1}, "12345"]}}
                       """, "/a/b/1", """
                       "00012345"\
                       """, """
                       "12345000"\
                       """),
                Arguments.of(
            """ 
                       {"a": {"b": "123456789"}}
                       """, "/a/b", """
                       "123456789"\
                       """, """
                       "123456789"\
                       """),
                Arguments.of(
                        """ 
                       {"a": {"b": ["12345","123"]}}
                       """, "/a/b", """
                       ["00012345","00000123"]\
                       """, """
                       ["12345000","12300000"]\
                       """)
        );
    }

    private JsonNode convert(String jsonString) throws Exception {
        return (new ObjectMapper()).readValue(jsonString, JsonNode.class);
    }

    @SpringBootApplication
    @ComponentScan
    protected static class PadNormalizerTestApplication {}
}
