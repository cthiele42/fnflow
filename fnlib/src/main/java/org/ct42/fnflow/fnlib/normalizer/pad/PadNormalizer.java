package org.ct42.fnflow.fnlib.normalizer.pad;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.springframework.stereotype.Component;

/**
 * It is a function for padding a text value in a JsonNode input object's specific path.
 *
 * @author Sajjad Safaeian
 */
@Component("padNormalizer")
public class PadNormalizer extends ConfigurableFunction<JsonNode, JsonNode, PadProperties> {

    @Override
    public JsonNode apply(JsonNode input) {
        JsonPointer pointer = properties.getElementPath();
        JsonNode resultNode = input.at(pointer);

        if(resultNode.isTextual()) {
            JsonNode parentNode = input.at(pointer.head());
            if(parentNode.isObject()) {
                ((ObjectNode) parentNode).put(pointer.last().getMatchingProperty(), padString(resultNode.asText()));
            }
        }

        if(resultNode.isArray()) {
            ArrayNode array = (ArrayNode) resultNode;
            for (int i = 0; i <array.size(); i++) {
                if(array.get(i).isTextual()) {
                    array.set(i, padString(array.get(i).asText()));
                }
            }
        }

        return input;
    }

    private String padString(String input) {
        return switch (properties.getPad()) {
            case LEFT -> StringUtils.leftPad(input, properties.getLength(), properties.getFillerCharacter());
            case RIGHT -> StringUtils.rightPad(input, properties.getLength(), properties.getFillerCharacter());
        };
    }
}
