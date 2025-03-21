package org.ct42.fnflow.fnlib.reducer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.ct42.fnflow.fnlib.NullProperties;
import org.springframework.stereotype.Component;

@Component("Reduce2One")
public class Reduce2One extends ConfigurableFunction<JsonNode, JsonNode, NullProperties> {
    @Override
    public JsonNode apply(JsonNode input) {
        JsonNode matches = input.get("matches");
        if(matches == null || !matches.isArray()) {
            throw new IllegalArgumentException("Invalid input, no matches array found");
        }
        if(matches.size() == 1) { // match
            //leave everything as it is
        } else if(matches.isEmpty()) { //no match
            ((ArrayNode)matches).add(JsonNodeFactory.instance.objectNode());
        } else { // ambiguous match
            ((ObjectNode)input).replace("input", JsonNodeFactory.instance.nullNode());
            ((ObjectNode)input).replace("matches", JsonNodeFactory.instance.arrayNode());
        }
        return input;
    }
}
