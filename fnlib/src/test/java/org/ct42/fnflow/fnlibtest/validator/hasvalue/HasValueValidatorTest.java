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