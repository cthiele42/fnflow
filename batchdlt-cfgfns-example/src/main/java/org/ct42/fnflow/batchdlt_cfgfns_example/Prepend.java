package org.ct42.fnflow.batchdlt_cfgfns_example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.springframework.stereotype.Component;

@Component("prepend")
public class Prepend extends ConfigurableFunction<JsonNode, JsonNode, PrependProperties> {
    @Override
    public JsonNode apply(JsonNode input) {
        ((ObjectNode)input).put("text", properties.getPrefix() + input.get("text").asText());
        return input;
    }
}
