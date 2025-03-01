package org.ct42.fnflow.fnlib.validator.hasvalue;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.ct42.fnflow.fnlib.validator.ValidationException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.stereotype.Component;

/**
 * It is a function to check if an element in a JsonNode input object's specific path has a value.
 *
 * @author Sajjad Safaeian
 */
@AutoConfiguration
@Component("hasValueValidator")
public class HasValueValidator extends ConfigurableFunction<JsonNode, JsonNode, HasValueProperties> {

    @Override
    public JsonNode apply(JsonNode input) {
        JsonPointer pointer = properties.getElementPath();
        JsonNode resultNode = input.at(pointer);

        if(null == resultNode || resultNode.isNull()) {
            throw new ValidationException("The value is null.");
        }

        if(resultNode.isMissingNode()) {
            throw new ValidationException("The element is not Exist.");
        }

        if(resultNode.isTextual() && StringUtils.isEmpty(resultNode.asText())) {
            throw new ValidationException("The value is empty text.");
        }

        if(resultNode.isArray()) {
            boolean containsValue = false;
            boolean hasValue = false;
            for (JsonNode node: resultNode) {
                if(node.isNumber() || node.isTextual() || node.isNull()) {
                    containsValue = true;
                    if(node.isNumber() || (node.isTextual() && StringUtils.isNotEmpty(node.asText()))) {
                        hasValue = true;
                    }
                }
            }

            if(containsValue && !hasValue) {
                throw new ValidationException("The array element has not value.");
            }
        }

        return input;
    }
}
